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

package org.wso2.carbon.identity.notification.push.provider.amazonsns.constants;

import java.util.Locale;

/**
 * Enumeration of supported SNS application platforms.
 */
public enum SNSPlatformApplication {

    APNS("apns", "APNS"),
    APNS_SANDBOX("apns_sandbox", "APNS_SANDBOX"),
    FCM("fcm", "GCM"),
    ADM("adm", "ADM"),
    BAIDU("baidu", "BAIDU"),
    WNS("wns", "WNS"),
    MPNS("mpns", "MPNS");

    /**
     * The internal reference name or shorthand key used within the local application logic.
     */
    private final String name;

    /**
     * The specific platform string required by AWS to construct the Platform Application ARN.
     */
    private final String awsPlatformName;

    /**
     * Constructor for SNSPlatformApplication.
     *
     * @param name            Internal reference name for the platform.
     * @param awsPlatformName AWS-specific platform name used in ARN construction.
     */
    SNSPlatformApplication(String name, String awsPlatformName) {

        this.name = name;
        this.awsPlatformName = awsPlatformName;
    }

    /**
     * Get the internal reference name of the platform.
     *
     * @return Internal reference name.
     */
    public String getName() {

        return name;
    }

    /**
     * Get the AWS-specific platform name.
     *
     * @return AWS platform name used in ARN construction.
     */
    public String getAwsPlatformName() {

        return awsPlatformName;
    }

    private static final SNSPlatformApplication[] ENUM_VALUES = values();

    /**
     * Get the SNSPlatformApplication by its internal reference name.
     *
     * @param name Internal reference name of the platform.
     * @return Matching {@link SNSPlatformApplication}, or null if not found.
     */
    public static SNSPlatformApplication getByName(String name) {

        name = name.trim().toLowerCase(Locale.ENGLISH);
        for (SNSPlatformApplication platform : ENUM_VALUES) {
            if (platform.getName().equalsIgnoreCase(name)) {
                return platform;
            }
        }
        return null;
    }

    /**
     * Get the SNSPlatformApplication by extracting the platform from an endpoint ARN.
     *
     * @param endpointArn AWS SNS endpoint ARN string.
     * @return Matching {@link SNSPlatformApplication}, or null if not found.
     */
    public static SNSPlatformApplication getByEndpointArn(String endpointArn) {

        if (endpointArn == null || endpointArn.isEmpty()) {
            return null;
        }
        // Split by "/" to extract platform from ARN format: arn:aws:sns:region:account:endpoint/PLATFORM/app/id
        String[] arnParts = endpointArn.split("/");
        if (arnParts.length < 2) {
            return null;
        }
        String platformName = arnParts[1]; // Platform is the second part after splitting

        for (SNSPlatformApplication platform : ENUM_VALUES) {
            if (platform.getAwsPlatformName().equalsIgnoreCase(platformName)) {
                return platform;
            }
        }
        return null;
    }
}
