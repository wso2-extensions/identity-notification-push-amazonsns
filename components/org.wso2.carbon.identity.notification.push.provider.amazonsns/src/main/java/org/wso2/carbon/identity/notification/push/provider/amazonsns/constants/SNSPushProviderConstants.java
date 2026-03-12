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

/**
 * Constants for Amazon SNS push notification provider.
 */
public class SNSPushProviderConstants {

    private SNSPushProviderConstants() {

    }

    public static final String SNS_PROVIDER_NAME = "AmazonSNS";
    public static final String SNS_SECRET_REFERENCE = "SNS-credentials";

    public static final String SNS_ACCESS_KEY_ID = "accessKeyId";
    public static final String SNS_SECRET_ACCESS_KEY = "secretAccessKey";
    public static final String SNS_REGION = "region";
    public static final String SNS_PLATFORM_ARNS = "platformArns";

    public static final String SNS_PLATFORM_DELIMITER = ":";

    public static final String SNS_METADATA_PLATFORM = "platform";
    public static final String SNS_METADATA_BAIDU_USERID = "userId";

    public static final String SNS_TOKEN = "Token";
    public static final String SNS_ENABLED = "Enabled";

    // Baidu specific constants
    public static final String SNS_BAIDU_USERID = "UserId";
    public static final String SNS_BAIDU_CHANNEL_ID = "ChannelId";
}
