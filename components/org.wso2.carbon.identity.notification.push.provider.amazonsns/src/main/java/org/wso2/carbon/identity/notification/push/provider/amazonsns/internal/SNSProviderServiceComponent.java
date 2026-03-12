/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.notification.push.provider.amazonsns.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.notification.push.provider.PushProvider;
import org.wso2.carbon.identity.notification.push.provider.amazonsns.impl.AmazonSNSPushProvider;
import org.wso2.carbon.identity.secret.mgt.core.SecretManager;
import org.wso2.carbon.identity.secret.mgt.core.SecretResolveManager;

/**
 * Push Provider Service Component.
 */
@Component(
        name = "org.wso2.carbon.identity.notification.push.provider.amazonsns",
        immediate = true
)
public class SNSProviderServiceComponent {

    private static final Log LOG = LogFactory.getLog(SNSProviderServiceComponent.class);

    @Activate
    protected void activate(ComponentContext context) {

        try {
            context.getBundleContext().registerService(PushProvider.class.getName(), new AmazonSNSPushProvider(), null);
        } catch (Throwable e) {
            LOG.error("Error occurred while activating Push Provider Service Component", e);
            return;
        }

        LOG.debug("SNS Push Provider Service Component bundle activated successfully.");
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Push Provider Service Component bundle is deactivated.");
        }
    }

    @Reference(
            name = "org.wso2.carbon.identity.secret.mgt.core.SecretManager",
            service = SecretManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetSecretManager"
    )
    protected void setSecretManager(SecretManager secretManager) {

        SNSProviderDataHolder.getInstance().setSecretManager(secretManager);
    }

    protected void unsetSecretManager(SecretManager secretManager) {

        SNSProviderDataHolder.getInstance().setSecretManager(null);
    }

    @Reference(
            name = "org.wso2.carbon.identity.secret.mgt.core.SecretResolveManager",
            service = SecretResolveManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetSecretResolveManager"
    )
    protected void setSecretResolveManager(SecretResolveManager secretResolveManager) {

        SNSProviderDataHolder.getInstance().setSecretResolveManager(secretResolveManager);
    }

    protected void unsetSecretResolveManager(SecretResolveManager secretResolveManager) {

        SNSProviderDataHolder.getInstance().setSecretResolveManager(null);
    }
}
