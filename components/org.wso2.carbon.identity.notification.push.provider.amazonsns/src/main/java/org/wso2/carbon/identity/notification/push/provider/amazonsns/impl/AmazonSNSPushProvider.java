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

package org.wso2.carbon.identity.notification.push.provider.amazonsns.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.notification.push.provider.PushProvider;
import org.wso2.carbon.identity.notification.push.provider.amazonsns.constants.NotificationTemplate;
import org.wso2.carbon.identity.notification.push.provider.amazonsns.constants.SNSPlatformApplication;
import org.wso2.carbon.identity.notification.push.provider.amazonsns.constants.SNSPushProviderConstants;
import org.wso2.carbon.identity.notification.push.provider.amazonsns.internal.SNSProviderDataHolder;
import org.wso2.carbon.identity.notification.push.provider.constant.PushProviderConstants;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderClientException;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderException;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderServerException;
import org.wso2.carbon.identity.notification.push.provider.model.PushDeviceData;
import org.wso2.carbon.identity.notification.push.provider.model.PushNotificationData;
import org.wso2.carbon.identity.notification.push.provider.model.PushSenderData;
import org.wso2.carbon.identity.secret.mgt.core.SecretManager;
import org.wso2.carbon.identity.secret.mgt.core.SecretResolveManager;
import org.wso2.carbon.identity.secret.mgt.core.exception.SecretManagementException;
import org.wso2.carbon.identity.secret.mgt.core.model.ResolvedSecret;
import org.wso2.carbon.identity.secret.mgt.core.model.Secret;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.AuthorizationErrorException;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.HashMap;
import java.util.Map;

import static org.wso2.carbon.identity.notification.push.provider.constant.PushProviderConstants.PUSH_PROVIDER_SECRET_TYPE;

/**
 * Amazon SNS push notification provider implementation.
 */
public class AmazonSNSPushProvider implements PushProvider {

    private static final Log log = LogFactory.getLog(AmazonSNSPushProvider.class);

    /**
     * Initializes the Amazon SNS Push Provider.
     */
    public AmazonSNSPushProvider() {

    }

    @Override
    public String getName() {

        return SNSPushProviderConstants.SNS_PROVIDER_NAME;
    }

    @Override
    public void sendNotification(PushNotificationData pushNotificationData, PushSenderData pushSenderData,
                                 String tenantDomain) throws PushProviderException {

        SNSPlatformApplication platform = SNSPlatformApplication.getByEndpointArn(
                pushNotificationData.getDeviceHandle());

        if (platform == null) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to determine platform from endpoint ARN: "
                        + sanitizeLogInput(pushNotificationData.getDeviceHandle()));
            }
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_PUSH_NOTIFICATION_SENDING_FAILED;
            throw new PushProviderServerException(error.getCode(), error.getMessage());
        }

        try (SnsClient snsClient = getSNSClient(pushSenderData.getProperties())) {

            // Build platform-specific notification messages
            String messagePayload = NotificationTemplate.buildNotificationMessages(
                    pushNotificationData.getNotificationTitle(),
                    pushNotificationData.getNotificationBody(),
                    pushNotificationData.getAdditionalData()
            );

            PublishRequest.Builder publishRequestBuilder = PublishRequest.builder()
                    .targetArn(pushNotificationData.getDeviceHandle())
                    .message(messagePayload)
                    .messageStructure("json");

            // Add message attributes for WNS and MPNS platforms
            addPlatformMessageAttributes(publishRequestBuilder, platform);

            PublishRequest publishRequest = publishRequestBuilder.build();

            snsClient.publish(publishRequest);

            if (log.isDebugEnabled()) {
                log.debug("Notification successfully sent to endpoint: "
                        + sanitizeLogInput(pushNotificationData.getDeviceHandle()));
            }

        } catch (InvalidParameterException e) {
            String errorMsg = e.awsErrorDetails().errorMessage();
            if (errorMsg.contains("TargetArn") && errorMsg.contains("No endpoint found")) {
                if (log.isDebugEnabled()) {
                    log.debug("The specified SNS endpoint was not found: "
                            + sanitizeLogInput(e.awsErrorDetails().errorMessage()));
                }
                PushProviderConstants.ErrorMessages error =
                        PushProviderConstants.ErrorMessages.ERROR_DEVICE_HANDLE_EXPIRED_OR_NEW_REGISTRATION_REQUIRED;
                throw new PushProviderServerException(error.getCode(), error.getMessage(), e);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Invalid parameter for SNS publish: "
                            + sanitizeLogInput(e.awsErrorDetails().errorMessage()));
                }
                PushProviderConstants.ErrorMessages error =
                        PushProviderConstants.ErrorMessages.ERROR_PUSH_NOTIFICATION_SENDING_FAILED;
                throw new PushProviderServerException(error.getCode(), error.getMessage(), e);
            }

        } catch (NotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Push notification send failed: "
                        + sanitizeLogInput(e.awsErrorDetails().errorMessage()), e);
            }
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_PUSH_NOTIFICATION_SENDING_FAILED;
            throw new PushProviderServerException(error.getCode(), error.getMessage(), e);
        } catch (SnsException e) {
            handleSNSException(e,
                    PushProviderConstants.ErrorMessages.ERROR_PUSH_NOTIFICATION_SENDING_FAILED);
        }
    }

    @Override
    public void registerDevice(PushDeviceData pushDeviceData, PushSenderData pushSenderData)
            throws PushProviderException {

        validateProviderMetadata(pushDeviceData.getProviderMetadata());

        SNSPlatformApplication platform = SNSPlatformApplication.getByName(
                pushDeviceData.getProviderMetadata().get(SNSPushProviderConstants.SNS_METADATA_PLATFORM));

        String platformApplicationArn = getPlatformArn(platform, pushSenderData.getProperties());

        try (SnsClient snsClient = getSNSClient(pushSenderData.getProperties())) {

            CreatePlatformEndpointRequest.Builder endpointRequestBuilder = CreatePlatformEndpointRequest.builder()
                    .token(pushDeviceData.getDeviceToken())
                    .platformApplicationArn(platformApplicationArn);

            // Additional attributes for Baidu platform
            if (platform == SNSPlatformApplication.BAIDU) {
                String baiduUserId = pushDeviceData.getProviderMetadata().get(
                        SNSPushProviderConstants.SNS_METADATA_BAIDU_USERID);

                Map<String, String> endpointAttributes = new HashMap<>();
                endpointAttributes.put(SNSPushProviderConstants.SNS_BAIDU_CHANNEL_ID,
                        pushDeviceData.getDeviceToken());
                endpointAttributes.put(SNSPushProviderConstants.SNS_BAIDU_USERID, baiduUserId);

                endpointRequestBuilder.attributes(endpointAttributes);
            }

            CreatePlatformEndpointRequest endpointRequest = endpointRequestBuilder.build();

            // Register the device with SNS to get the endpoint ARN
            CreatePlatformEndpointResponse response = snsClient.createPlatformEndpoint(endpointRequest);

            // Set the device handle as the endpoint ARN
            pushDeviceData.setDeviceHandle(response.endpointArn());
            if (log.isDebugEnabled()) {
                log.debug("Device successfully registered with Amazon SNS. Endpoint ARN: " +
                        sanitizeLogInput(response.endpointArn()));
            }

        } catch (InvalidParameterException e) {
            // Check if the error is due to an existing endpoint with different attributes
            String message = e.awsErrorDetails().errorMessage();
            if (message.contains("Endpoint already exists")) {
                int endpointIndex = message.indexOf("EndpointArn: ");
                if (endpointIndex != -1) {
                    String existingEndpointArn = message.substring(endpointIndex + 13).trim();
                    pushDeviceData.setDeviceHandle(existingEndpointArn);
                    if (log.isDebugEnabled()) {
                        log.debug("Device is already registered with SNS. Using existing endpoint ARN: " +
                                sanitizeLogInput(existingEndpointArn));
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Endpoint already exists but could not extract ARN: "
                                + sanitizeLogInput(message));
                    }
                    PushProviderConstants.ErrorMessages error =
                            PushProviderConstants.ErrorMessages.ERROR_PUSH_DEVICE_REGISTRATION_FAILED;
                    throw new PushProviderServerException(error.getCode(), error.getMessage(), e);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Invalid parameter for device registration: "
                            + sanitizeLogInput(e.awsErrorDetails().errorMessage()));
                }
                PushProviderConstants.ErrorMessages error =
                        PushProviderConstants.ErrorMessages.ERROR_PUSH_DEVICE_REGISTRATION_FAILED;
                throw new PushProviderClientException(error.getCode(), error.getMessage(), e);
            }
        } catch (NotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("The specified platform application ARN was not found: "
                        + sanitizeLogInput(e.awsErrorDetails().errorMessage()), e);
            }
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_PUSH_DEVICE_REGISTRATION_FAILED;
            throw new PushProviderServerException(error.getCode(), error.getMessage(), e);
        } catch (SnsException e) {
            handleSNSException(e,
                    PushProviderConstants.ErrorMessages.ERROR_PUSH_DEVICE_REGISTRATION_FAILED);
        }
    }

    @Override
    public void unregisterDevice(PushDeviceData pushDeviceData, PushSenderData pushSenderData)
            throws PushProviderException {

        // Validations
        if (StringUtils.isBlank(pushDeviceData.getDeviceHandle())) {
            if (log.isDebugEnabled()) {
                log.debug("Device handle is missing. Skipping SNS endpoint deletion.");
            }
            return;
        }

        try (SnsClient snsClient = getSNSClient(pushSenderData.getProperties())) {

            snsClient.deleteEndpoint(builder -> builder.endpointArn(pushDeviceData.getDeviceHandle()));

            if (log.isDebugEnabled()) {
                log.debug("Device successfully unregistered from Amazon SNS.");
            }
        } catch (SnsException e) {
            handleSNSException(e,
                    PushProviderConstants.ErrorMessages.ERROR_PUSH_DEVICE_UNREGISTRATION_FAILED);
        }
    }

    @Override
    public void updateDevice(PushDeviceData pushDeviceData, PushSenderData pushSenderData)
            throws PushProviderException {

        SNSPlatformApplication platform = SNSPlatformApplication.getByEndpointArn(pushDeviceData.getDeviceHandle());

        if (platform == null) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to determine platform from endpoint ARN: "
                        + sanitizeLogInput(pushDeviceData.getDeviceHandle()));
            }
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_PUSH_DEVICE_UPDATE_FAILED;
            throw new PushProviderServerException(error.getCode(), error.getMessage());
        }

        try (SnsClient snsClient = getSNSClient(pushSenderData.getProperties())) {

            Map<String, String> endpointAttributes = getUpdateEndpointAttributes(pushDeviceData, platform);

            snsClient.setEndpointAttributes(builder -> builder
                    .endpointArn(pushDeviceData.getDeviceHandle())
                    .attributes(endpointAttributes));

            if (log.isDebugEnabled()) {
                log.debug("Device successfully updated in Amazon SNS.");
            }
        } catch (NotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("The specified SNS endpoint was not found: "
                        + sanitizeLogInput(e.awsErrorDetails().errorMessage()));
            }
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_DEVICE_HANDLE_EXPIRED_OR_NEW_REGISTRATION_REQUIRED;
            throw new PushProviderClientException(error.getCode(), error.getMessage(), e);

        } catch (SnsException e) {
            handleSNSException(e,
                    PushProviderConstants.ErrorMessages.ERROR_PUSH_DEVICE_UPDATE_FAILED);
        }
    }

    @Override
    public Map<String, String> preProcessProperties(PushSenderData pushSenderData) throws PushProviderException {

        if (log.isDebugEnabled()) {
            log.debug("Pre-processing properties for providerId: "
                    + sanitizeLogInput(pushSenderData.getProviderId()));
        }

        if (!validateSNSCredentials(pushSenderData.getProperties())) {
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_REQUIRED_PROPERTY_MISSING;
            throw new PushProviderServerException(error.getCode(), error.getMessage());
        }

        return pushSenderData.getProperties();
    }

    @Override
    public Map<String, String> postProcessProperties(PushSenderData pushSenderData) throws PushProviderException {

        // Nothing to post process for SNS. Return the properties as is.
        if (log.isDebugEnabled()) {
            log.debug("No Post-processing properties needed for providerId: "
                    + sanitizeLogInput(pushSenderData.getProviderId()));
        }

        return pushSenderData.getProperties();
    }

    @Override
    public void updateCredentials(PushSenderData pushSenderData, String s) throws PushProviderException {

        // No-op for SNS as credentials are managed via secret manager.
    }

    @Override
    public Map<String, String> storePushProviderSecretProperties(PushSenderData pushSenderData)
            throws PushProviderException {

        if (log.isDebugEnabled()) {
            log.debug("Storing push provider secret properties for providerId: "
                    + sanitizeLogInput(pushSenderData.getProviderId()));
        }
        try {
            Map<String, String> properties = new HashMap<>(pushSenderData.getProperties());

            if (StringUtils.isBlank(properties.get(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY))) {
                log.debug("SNS Secret Access Key is missing in properties.");
                PushProviderConstants.ErrorMessages error =
                        PushProviderConstants.ErrorMessages.ERROR_REQUIRED_PROPERTY_MISSING;
                throw new PushProviderServerException(error.getCode(),
                        error.getMessage() + SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY);
            }

            String secretAccessKey = properties.get(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY);

            SecretManager secretManager = SNSProviderDataHolder.getInstance().getSecretManager();
            if (secretManager.isSecretExist(PUSH_PROVIDER_SECRET_TYPE,
                    SNSPushProviderConstants.SNS_SECRET_REFERENCE)) {
                log.debug("Updating existing secret in secret manager.");
                // Update the existing secret.
                secretManager.updateSecretValue(PUSH_PROVIDER_SECRET_TYPE,
                        SNSPushProviderConstants.SNS_SECRET_REFERENCE, secretAccessKey);
            } else {
                log.debug("Adding new secret to secret manager.");
                // Add the new secret.
                Secret newSecret = new Secret();
                newSecret.setSecretType(PUSH_PROVIDER_SECRET_TYPE);
                newSecret.setSecretName(SNSPushProviderConstants.SNS_SECRET_REFERENCE);
                newSecret.setSecretValue(secretAccessKey);
                secretManager.addSecret(PUSH_PROVIDER_SECRET_TYPE, newSecret);
            }
            properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY,
                    SNSPushProviderConstants.SNS_SECRET_REFERENCE);

            return properties;

        } catch (SecretManagementException e) {
            log.debug("Error occurred while storing secrets.");
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_WHILE_STORING_SECRETS_OF_PUSH_PROVIDER;
            throw new PushProviderServerException(error.getCode(), error.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> retrievePushProviderSecretProperties(PushSenderData pushSenderData)
            throws PushProviderException {

        if (log.isDebugEnabled()) {
            log.debug("Retrieving push provider secret properties for providerId: "
                    + sanitizeLogInput(pushSenderData.getProviderId()));
        }
        try {
            Map<String, String> properties = new HashMap<>(pushSenderData.getProperties());
            String secretReference = properties.get(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY);
            if (StringUtils.isBlank(secretReference)) {
                log.debug("SNS Secret Access Key reference is missing in properties.");
                PushProviderConstants.ErrorMessages error =
                        PushProviderConstants.ErrorMessages.ERROR_REQUIRED_PROPERTY_MISSING;
                throw new PushProviderServerException(error.getCode(),
                        error.getMessage() + SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY);
            }

            SecretManager secretManager = SNSProviderDataHolder.getInstance().getSecretManager();
            SecretResolveManager secretResolveManager = SNSProviderDataHolder.getInstance().getSecretResolveManager();
            if (secretManager.isSecretExist(PUSH_PROVIDER_SECRET_TYPE, secretReference)) {
                log.debug("Secret exists. Resolving secret value.");
                ResolvedSecret resolvedSecret =
                        secretResolveManager.getResolvedSecret(PUSH_PROVIDER_SECRET_TYPE, secretReference);

                String secretAccessKey = resolvedSecret.getResolvedSecretValue();

                properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, secretAccessKey);

                return properties;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Secret does not exist in secret manager: "
                            + sanitizeLogInput(secretReference));
                }
                PushProviderConstants.ErrorMessages error =
                        PushProviderConstants.ErrorMessages.ERROR_WHILE_RETRIEVING_SECRETS_OF_PUSH_PROVIDER;
                throw new PushProviderServerException(error.getCode(), error.getMessage());
            }
        } catch (SecretManagementException e) {
            log.debug("Error occurred while retrieving secrets.");
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_WHILE_RETRIEVING_SECRETS_OF_PUSH_PROVIDER;
            throw new PushProviderServerException(error.getCode(), error.getMessage(), e);
        }
    }

    @Override
    public void deletePushProviderSecretProperties(PushSenderData pushSenderData) throws PushProviderException {

        if (log.isDebugEnabled()) {
            log.debug("Deleting push provider secret properties for providerId: "
                    + sanitizeLogInput(pushSenderData.getProviderId()));
        }
        try {
            Map<String, String> properties = pushSenderData.getProperties();
            String secretReference = properties.get(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY);

            if (StringUtils.isBlank(secretReference)) {
                log.debug("No secret reference found in properties. No deletion required.");
                return;
            }

            SecretManager secretManager = SNSProviderDataHolder.getInstance().getSecretManager();
            if (secretManager.isSecretExist(PUSH_PROVIDER_SECRET_TYPE, secretReference)) {
                log.debug("Secret exists. Deleting secret from secret manager.");
                secretManager.deleteSecret(PUSH_PROVIDER_SECRET_TYPE, secretReference);
            } else {
                log.debug("Secret does not exist in secret manager. No deletion required.");
            }
        } catch (SecretManagementException e) {
            log.debug("Error occurred while deleting secrets.");
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_WHILE_DELETING_SECRETS_OF_PUSH_PROVIDER;
            throw new PushProviderServerException(error.getCode(), error.getMessage(), e);
        }
    }

    /**
     * Add platform-specific message attributes for WNS and MPNS platforms to the publish request.
     *
     * @param publishRequestBuilder The publish request builder to add attributes to.
     * @param platform              The SNS platform application.
     */
    private void addPlatformMessageAttributes(PublishRequest.Builder publishRequestBuilder,
                                              SNSPlatformApplication platform) {

        if (platform == SNSPlatformApplication.WNS) {
            Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
            messageAttributes.put("AWS.SNS.MOBILE.WNS.Type",
                    MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue("wns/toast")
                            .build());
            publishRequestBuilder.messageAttributes(messageAttributes);
        } else if (platform == SNSPlatformApplication.MPNS) {
            Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
            messageAttributes.put("AWS.SNS.MOBILE.MPNS.Type",
                    MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue("token")
                            .build());
            publishRequestBuilder.messageAttributes(messageAttributes);
        }
    }

    /**
     * Validate that the required SNS credentials are present in the given properties.
     *
     * @param properties Map containing the SNS provider properties to validate.
     * @return {@code true} if all required credentials are present, {@code false} otherwise.
     * @throws PushProviderException If an error occurs during validation.
     */
    private boolean validateSNSCredentials(Map<String, String> properties) throws PushProviderException {

        boolean isValid = true;
        if (StringUtils.isBlank(properties.get(SNSPushProviderConstants.SNS_ACCESS_KEY_ID))) {
            log.debug("SNS Access Key ID is missing in properties.");
            isValid = false;
        }
        if (StringUtils.isBlank(properties.get(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY))) {
            log.debug("SNS Secret Access Key is missing in properties.");
            isValid = false;
        }
        if (StringUtils.isBlank(properties.get(SNSPushProviderConstants.SNS_REGION))) {
            log.debug("SNS Region is missing in properties.");
            isValid = false;
        }
        return isValid;
    }

    /**
     * Validate the provider metadata required for device registration.
     *
     * @param providerMetadata Map containing the provider metadata to validate.
     * @throws PushProviderException If required metadata is missing or invalid.
     */
    private void validateProviderMetadata(Map<String, String> providerMetadata) throws PushProviderException {

        if (providerMetadata == null || providerMetadata.isEmpty()) {
            log.debug("Provider metadata is missing.");
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_REQUIRED_METADATA_MISSING;
            throw new PushProviderClientException(error.getCode(), error.getMessage() + " metadata attributes");
        }

        String platformName = providerMetadata.get(SNSPushProviderConstants.SNS_METADATA_PLATFORM);
        if (StringUtils.isBlank(platformName)) {
            log.debug("Platform is missing in provider metadata.");
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_REQUIRED_METADATA_MISSING;
            throw new PushProviderClientException(
                    error.getCode(), error.getMessage() + " platform");
        }

        SNSPlatformApplication platform = SNSPlatformApplication.getByName(platformName);
        if (platform == null) {
            if (log.isDebugEnabled()) {
                log.debug("Invalid platform name in provider metadata: " + sanitizeLogInput(platformName));
            }
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_REQUIRED_METADATA_MISSING;
            throw new PushProviderClientException(error.getCode(),
                    error.getMessage() + " Valid platform");
        }

        if (platform == SNSPlatformApplication.BAIDU &&
                StringUtils.isBlank(providerMetadata.get(SNSPushProviderConstants.SNS_METADATA_BAIDU_USERID))) {
            log.debug("Baidu User ID is missing in provider metadata for Baidu platform.");
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_REQUIRED_METADATA_MISSING;
            throw new PushProviderClientException(error.getCode(),
                    error.getMessage() + " Baidu User ID for Baidu platform");
        }
    }

    /**
     * Retrieve the platform application ARN for the given platform from the properties.
     *
     * @param platform   The SNS platform application.
     * @param properties Map containing the provider properties.
     * @return The platform application ARN.
     * @throws PushProviderException If the platform ARN is not found in the properties.
     */
    private String getPlatformArn(SNSPlatformApplication platform, Map<String, String> properties)
            throws PushProviderException {

        String platformArnKey = SNSPushProviderConstants.SNS_PLATFORM_ARNS +
                SNSPushProviderConstants.SNS_PLATFORM_DELIMITER + platform.getName();
        String platformArnValue = properties.get(platformArnKey);
        if (StringUtils.isNotEmpty(platformArnValue)) {
            return platformArnValue;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Platform ARN not found for platform: " + sanitizeLogInput(platform.getName()));
            }
            PushProviderConstants.ErrorMessages error =
                    PushProviderConstants.ErrorMessages.ERROR_REQUIRED_PROPERTY_MISSING;
            throw new PushProviderServerException(error.getCode(),
                    error.getMessage() + " Platform ARN for " + platform.getName());
        }
    }

    /**
     * Build the endpoint attributes map for updating a device endpoint.
     *
     * @param pushDeviceData The push device data containing the device token.
     * @param platform       The SNS platform application.
     * @return Map of endpoint attributes to update.
     */
    private static Map<String, String> getUpdateEndpointAttributes(
            PushDeviceData pushDeviceData, SNSPlatformApplication platform) {

        Map<String, String> endpointAttributes = new HashMap<>();
        endpointAttributes.put(SNSPushProviderConstants.SNS_TOKEN, pushDeviceData.getDeviceToken());
        endpointAttributes.put(SNSPushProviderConstants.SNS_ENABLED, "true");

        // Additional attributes for Baidu platform
        if (platform == SNSPlatformApplication.BAIDU) {
            endpointAttributes.put(SNSPushProviderConstants.SNS_BAIDU_CHANNEL_ID,
                    pushDeviceData.getDeviceToken());
        }
        return endpointAttributes;
    }

    /**
     * Create and return an Amazon SNS client configured with the given properties.
     *
     * @param properties Map containing SNS access key ID, secret access key, and region.
     * @return Configured {@link SnsClient} instance.
     * @throws PushProviderException If the client cannot be created.
     */
    protected SnsClient getSNSClient(Map<String, String> properties) throws PushProviderException {

        String accessKeyId = properties.get(SNSPushProviderConstants.SNS_ACCESS_KEY_ID);
        String secretAccessKey = properties.get(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY);
        String region = properties.get(SNSPushProviderConstants.SNS_REGION);

        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        );

        return SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .httpClientBuilder(Apache5HttpClient.builder())
                .build();
    }

    /**
     * Sanitize the input string for safe logging by replacing newline characters.
     *
     * @param input The input string to sanitize.
     * @return The sanitized string with newline characters replaced, or {@code null} if input is {@code null}.
     */
    private static String sanitizeLogInput(String input) {

        if (StringUtils.isBlank(input)) {
            return null;
        }
        return input.replaceAll("[\r\n]", "_");
    }

    /**
     * Handle SNS exceptions by logging the error details and throwing a push provider exception.
     *
     * @param e            The SNS exception to handle.
     * @param defaultError The default error message to use for the thrown exception.
     * @throws PushProviderException Always thrown with the provided error details.
     */
    private void handleSNSException(SnsException e,
                                    PushProviderConstants.ErrorMessages defaultError)
            throws PushProviderException {

        if (e instanceof AuthorizationErrorException) {
            if (log.isDebugEnabled()) {
                log.debug("Authorization error occurred while interacting with Amazon SNS: "
                        + sanitizeLogInput(e.awsErrorDetails().errorMessage()), e);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Error occurred while interacting with Amazon SNS: "
                        + sanitizeLogInput(e.awsErrorDetails().errorMessage()), e);
            }
        }
        throw new PushProviderServerException(defaultError.getCode(), defaultError.getMessage(), e);
    }
}
