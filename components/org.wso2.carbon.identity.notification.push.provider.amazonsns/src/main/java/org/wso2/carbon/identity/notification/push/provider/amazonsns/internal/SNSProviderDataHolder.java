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

import org.wso2.carbon.identity.secret.mgt.core.SecretManager;
import org.wso2.carbon.identity.secret.mgt.core.SecretResolveManager;

/**
 * Push Provider Data Holder.
 */
public class SNSProviderDataHolder {

    private SecretManager secretManager;
    private SecretResolveManager secretResolveManager;
    private static SNSProviderDataHolder instance = new SNSProviderDataHolder();

    private SNSProviderDataHolder() {

    }

    /**
     * Get the singleton instance of SNSProviderDataHolder.
     *
     * @return Singleton {@link SNSProviderDataHolder} instance.
     */
    public static SNSProviderDataHolder getInstance() {

        return instance;
    }

    /**
     * Get Secret Manager.
     *
     * @return Secret Manager.
     */
    public SecretManager getSecretManager() {

        return secretManager;
    }

    /**
     * Set Secret Manager.
     *
     * @param secretManager Secret Manager.
     */
    public void setSecretManager(SecretManager secretManager) {

        this.secretManager = secretManager;
    }

    /**
     * Get Secret Resolve Manager.
     *
     * @return Secret Resolve Manager.
     */
    public SecretResolveManager getSecretResolveManager() {

        return secretResolveManager;
    }

    /**
     * Set Secret Resolve Manager.
     *
     * @param secretResolveManager Secret Resolve Manager.
     */
    public void setSecretResolveManager(SecretResolveManager secretResolveManager) {

        this.secretResolveManager = secretResolveManager;
    }
}
