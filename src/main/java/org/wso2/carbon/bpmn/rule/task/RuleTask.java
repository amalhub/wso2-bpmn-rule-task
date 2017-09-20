/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.bpmn.rule.task;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.bpmn.rule.task.internal.RuleTaskDataHolder;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.registry.api.Registry;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.api.Resource;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Drools rule task implementation using BPMN service task.
 */
public class RuleTask implements JavaDelegate {

    private static final Log log = LogFactory.getLog(RuleTask.class);

    private static final String DRL_REG_PATH_VAR_NAME = "drlRegPath";
    private static final String GOV_REGISTRY_PREFIX = "gov:/";
    private static final String CONF_REGISTRY_PREFIX = "conf:/";
    private static final String GLOBAL_PREFIX = "global_";
    private static final String FACT_PREFIX = "fact_";

    private Expression drlRegPath;

    public void setDrlRegPath(Expression drlRegPath) {
        this.drlRegPath = drlRegPath;
    }

    public void execute(DelegateExecution execution) throws Exception {
        try {
            if (drlRegPath == null) {
                throw new RuntimeException("Drools file registry path not found!");
            }
            String drlRegPathValue = (String) drlRegPath.getValue(execution);
            if (RuleTaskUtils.isEmpty(drlRegPathValue)) {
                throw new RuntimeException("Field " + DRL_REG_PATH_VAR_NAME + " is empty!");
            }

            InputStream inputStream = readRegResource(execution, drlRegPathValue);
            Map<String, Object> globals = prepareGlobals(execution);
            List<Object> facts = prepareFacts(execution);
            RuleExecutor.execute(inputStream, globals, facts);
        } catch (Exception e) {
            // Log error here since the caller does not log it
            log.error(e);
            throw e;
        }
    }

    private Map<String, Object> prepareGlobals(DelegateExecution execution) {
        Map<String, Object> globals = new HashedMap();
        for(String key : execution.getVariables().keySet()) {
            if(key.startsWith(GLOBAL_PREFIX)) {
                Object obj = execution.getVariable(key);
                if(obj instanceof Long) {
                    // Drools requires Integer and BPMN uses Long, hence cast
                    obj = Integer.valueOf(String.valueOf(obj));
                }
                if(log.isDebugEnabled()) {
                    log.debug("Adding global: " + key + " = "+ obj);
                }
                globals.put(key.replace(GLOBAL_PREFIX, ""), obj);
            }
        }
        return globals;
    }

    private List<Object> prepareFacts(DelegateExecution execution) {
        List<Object> facts = new ArrayList<Object>();
        for(String key : execution.getVariables().keySet()) {
            if(key.startsWith(FACT_PREFIX)) {
                if(log.isDebugEnabled()) {
                    log.debug("Adding fact: " + key + " = "+ execution.getVariable(key));
                }
                facts.add(execution.getVariable(key));
            }
        }
        return facts;
    }

    private InputStream readRegResource(DelegateExecution execution, String resourcePath) throws UserStoreException, RegistryException {

        if(log.isDebugEnabled()) {
            log.debug("Reading registry resource: " + resourcePath);
        }

        int tenantIdInt = Integer.parseInt(execution.getTenantId());
        RealmService realmService = RegistryContext.getBaseInstance().getRealmService();
        String domain = realmService.getTenantManager().getDomain(tenantIdInt);

        PrivilegedCarbonContext.startTenantFlow();
        if (domain != null) {
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(domain);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantIdInt);
        } else {
            PrivilegedCarbonContext.getThreadLocalCarbonContext()
                    .setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantIdInt);
        }

        String registryPath;
        Registry registry;
        if (resourcePath.startsWith(GOV_REGISTRY_PREFIX)) {
            registryPath = resourcePath.substring(GOV_REGISTRY_PREFIX.length());
            registry = RuleTaskDataHolder.getInstance().getRegistryService().getGovernanceSystemRegistry(tenantIdInt);
        } else if (resourcePath.startsWith(CONF_REGISTRY_PREFIX)) {
            registryPath = resourcePath.substring(CONF_REGISTRY_PREFIX.length());
            registry = RuleTaskDataHolder.getInstance().getRegistryService().getConfigSystemRegistry(tenantIdInt);
        } else {
            String msg = DRL_REG_PATH_VAR_NAME + " value should begin with " + GOV_REGISTRY_PREFIX + " or "
                    + CONF_REGISTRY_PREFIX + ".";
            throw new RuntimeException(msg);
        }

        if (log.isDebugEnabled()) {
            log.debug("Reading drools file from registry path " + registryPath + " for task "
                    + getTaskDetails(execution));
        }

        Resource urlResource = registry.get(registryPath);
        return urlResource.getContentStream();
    }

    private String getTaskDetails(DelegateExecution execution) {
        String task = execution.getCurrentActivityId() + ":" + execution.getCurrentActivityName()
                + " in process instance " + execution.getProcessInstanceId();
        return task;
    }
}
