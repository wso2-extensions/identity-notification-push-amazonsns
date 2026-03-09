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

package org.wso2.carbon.identity.notification.push.provider.sns.impl;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderClientException;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderException;
import org.wso2.carbon.identity.notification.push.provider.exception.PushProviderServerException;
import org.wso2.carbon.identity.notification.push.provider.model.PushDeviceData;
import org.wso2.carbon.identity.notification.push.provider.model.PushNotificationData;
import org.wso2.carbon.identity.notification.push.provider.model.PushSenderData;
import org.wso2.carbon.identity.notification.push.provider.sns.constants.NotificationTemplates;
import org.wso2.carbon.identity.notification.push.provider.sns.constants.SNSPushProviderConstants;
import org.wso2.carbon.identity.notification.push.provider.sns.internal.SNSProviderDataHolder;
import org.wso2.carbon.identity.secret.mgt.core.SecretManager;
import org.wso2.carbon.identity.secret.mgt.core.SecretResolveManager;
import org.wso2.carbon.identity.secret.mgt.core.exception.SecretManagementException;
import org.wso2.carbon.identity.secret.mgt.core.model.ResolvedSecret;
import org.wso2.carbon.identity.secret.mgt.core.model.SecretType;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.AuthorizationErrorException;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse;
import software.amazon.awssdk.services.sns.model.DeleteEndpointResponse;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SetEndpointAttributesResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.wso2.carbon.identity.notification.push.provider.constant.PushProviderConstants.PUSH_PROVIDER_SECRET_TYPE;

/**
 * Amazon SNS Push Provider Test.
 */
public class AmazonSNSPushProviderTest {

    private static final String TEST_ACCESS_KEY_ID = "testAccessKeyId";
    private static final String TEST_SECRET_ACCESS_KEY = "testSecretAccessKey";
    private static final String TEST_REGION = "us-east-1";
    private static final String TEST_DEVICE_TOKEN = "testDeviceToken";
    private static final String TEST_ENDPOINT_ARN =
            "arn:aws:sns:us-east-1:123456789012:endpoint/GCM/MyApp/12345678-1234-1234-1234-123456789012";
    private static final String TEST_PLATFORM_ARN_FCM =
            "arn:aws:sns:us-east-1:123456789012:app/GCM/MyApp";
    private static final String TEST_PLATFORM_ARN_BAIDU =
            "arn:aws:sns:us-east-1:123456789012:app/BAIDU/MyApp";
    private static final String TEST_WNS_ENDPOINT_ARN =
            "arn:aws:sns:us-east-1:123456789012:endpoint/WNS/MyApp/12345678-1234-1234-1234-123456789012";
    private static final String TEST_MPNS_ENDPOINT_ARN =
            "arn:aws:sns:us-east-1:123456789012:endpoint/MPNS/MyApp/12345678-1234-1234-1234-123456789012";
    private static final String TEST_NOTIFICATION_TITLE = "Test Notification";
    private static final String TEST_NOTIFICATION_BODY = "Test Body";
    private static final String TEST_PROVIDER_ID = "testProviderId";
    private static final String TEST_BAIDU_USER_ID = "testBaiduUserId";
    private static final String TEST_MESSAGE_PAYLOAD = "{\"default\":\"Test Message\"}";

    private AmazonSNSPushProvider snsPushProvider;

    @Mock
    private PushSenderData pushSenderData = Mockito.mock(PushSenderData.class);

    @Mock
    private SnsClient snsClient = Mockito.mock(SnsClient.class);

    @BeforeTest
    public void setUp() {
        snsPushProvider = new AmazonSNSPushProvider();
    }

    @BeforeMethod
    public void resetMocks() {
        Mockito.reset(pushSenderData, snsClient);
    }

    // ==================== Helper Class for Testing ====================

    /**
     * Testable version of AmazonSNSPushProvider that allows injecting mock SnsClient.
     */
    private class TestableAmazonSNSPushProvider extends AmazonSNSPushProvider {
        private final SnsClient mockSnsClient;

        public TestableAmazonSNSPushProvider(SnsClient mockSnsClient) {
            this.mockSnsClient = mockSnsClient;
        }

        @Override
        public void sendNotification(PushNotificationData pushNotificationData, PushSenderData pushSenderData,
                                     String tenantDomain) throws PushProviderException {
            // Temporarily override to use mock client
            super.sendNotification(pushNotificationData, pushSenderData, tenantDomain);
        }

        @Override
        public void registerDevice(PushDeviceData pushDeviceData, PushSenderData pushSenderData)
                throws PushProviderException {
            super.registerDevice(pushDeviceData, pushSenderData);
        }

        @Override
        public void unregisterDevice(PushDeviceData pushDeviceData, PushSenderData pushSenderData)
                throws PushProviderException {
            super.unregisterDevice(pushDeviceData, pushSenderData);
        }

        @Override
        public void updateDevice(PushDeviceData pushDeviceData, PushSenderData pushSenderData)
                throws PushProviderException {
            super.updateDevice(pushDeviceData, pushSenderData);
        }

        // Override to inject mock client
        @Override
        protected SnsClient getSNSClient(Map<String, String> properties) throws PushProviderException {
            return mockSnsClient;
        }
    }

    // ==================== Test getName() ====================

    @Test(priority = 1)
    public void testGetName() {
        String name = snsPushProvider.getName();
        Assert.assertNotNull(name);
        Assert.assertEquals(name, SNSPushProviderConstants.SNS_PROVIDER_NAME);
    }

    // ==================== Test sendNotification() - Success Cases ====================

    @Test(priority = 2)
    public void testSendNotificationSuccess() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        try (MockedStatic<NotificationTemplates> mockedTemplates = Mockito.mockStatic(NotificationTemplates.class)) {
            mockedTemplates.when(() -> NotificationTemplates.buildNotificationMessages(
                    anyString(), anyString(), any(Map.class))).thenReturn(TEST_MESSAGE_PAYLOAD);

            PushNotificationData notificationData = new PushNotificationData.Builder()
                    .setNotificationTitle(TEST_NOTIFICATION_TITLE)
                    .setNotificationBody(TEST_NOTIFICATION_BODY)
                    .setDeviceHandle(TEST_ENDPOINT_ARN)
                    .build();

            PublishResponse publishResponse = PublishResponse.builder()
                    .messageId("testMessageId")
                    .build();
            when(snsClient.publish(any(PublishRequest.class))).thenReturn(publishResponse);
            doNothing().when(snsClient).close();

            testProvider.sendNotification(notificationData, pushSenderData, "carbon.super");

            verify(snsClient, times(1)).publish(any(PublishRequest.class));
            verify(snsClient, times(1)).close();
        }
    }

    @Test(priority = 3)
    public void testSendNotificationSuccessWithWNSPlatform() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        try (MockedStatic<NotificationTemplates> mockedTemplates = Mockito.mockStatic(NotificationTemplates.class)) {
            mockedTemplates.when(() -> NotificationTemplates.buildNotificationMessages(
                    anyString(), anyString(), any(Map.class))).thenReturn(TEST_MESSAGE_PAYLOAD);

            PushNotificationData notificationData = new PushNotificationData.Builder()
                    .setNotificationTitle(TEST_NOTIFICATION_TITLE)
                    .setNotificationBody(TEST_NOTIFICATION_BODY)
                    .setDeviceHandle(TEST_WNS_ENDPOINT_ARN)
                    .build();

            PublishResponse publishResponse = PublishResponse.builder()
                    .messageId("testMessageId")
                    .build();
            when(snsClient.publish(any(PublishRequest.class))).thenReturn(publishResponse);
            doNothing().when(snsClient).close();

            testProvider.sendNotification(notificationData, pushSenderData, "carbon.super");

            verify(snsClient, times(1)).publish(any(PublishRequest.class));
        }
    }

    @Test(priority = 4)
    public void testSendNotificationSuccessWithMPNSPlatform() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        try (MockedStatic<NotificationTemplates> mockedTemplates = Mockito.mockStatic(NotificationTemplates.class)) {
            mockedTemplates.when(() -> NotificationTemplates.buildNotificationMessages(
                    anyString(), anyString(), any(Map.class))).thenReturn(TEST_MESSAGE_PAYLOAD);

            PushNotificationData notificationData = new PushNotificationData.Builder()
                    .setNotificationTitle(TEST_NOTIFICATION_TITLE)
                    .setNotificationBody(TEST_NOTIFICATION_BODY)
                    .setDeviceHandle(TEST_MPNS_ENDPOINT_ARN)
                    .build();

            PublishResponse publishResponse = PublishResponse.builder()
                    .messageId("testMessageId")
                    .build();
            when(snsClient.publish(any(PublishRequest.class))).thenReturn(publishResponse);
            doNothing().when(snsClient).close();

            testProvider.sendNotification(notificationData, pushSenderData, "carbon.super");

            verify(snsClient, times(1)).publish(any(PublishRequest.class));
        }
    }

    @Test(priority = 5)
    public void testSendNotificationWithAdditionalData() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        try (MockedStatic<NotificationTemplates> mockedTemplates = Mockito.mockStatic(NotificationTemplates.class)) {
            mockedTemplates.when(() -> NotificationTemplates.buildNotificationMessages(
                    anyString(), anyString(), any(Map.class))).thenReturn(TEST_MESSAGE_PAYLOAD);

            PushNotificationData notificationData = new PushNotificationData.Builder()
                    .setNotificationTitle(TEST_NOTIFICATION_TITLE)
                    .setNotificationBody(TEST_NOTIFICATION_BODY)
                    .setDeviceHandle(TEST_ENDPOINT_ARN)
                    .setUsername("testUser")
                    .setTenantDomain("carbon.super")
                    .setApplicationName("TestApp")
                    .setNotificationScenario("login")
                    .setPushId("push123")
                    .setChallenge("challenge123")
                    .setIpAddress("192.168.1.1")
                    .setDeviceOS("Android")
                    .setBrowser("Chrome")
                    .setDeviceId("device123")
                    .build();

            PublishResponse publishResponse = PublishResponse.builder()
                    .messageId("testMessageId")
                    .build();
            when(snsClient.publish(any(PublishRequest.class))).thenReturn(publishResponse);
            doNothing().when(snsClient).close();

            testProvider.sendNotification(notificationData, pushSenderData, "carbon.super");

            verify(snsClient, times(1)).publish(any(PublishRequest.class));
        }
    }

    // ==================== Test sendNotification() - Failure Cases ====================

    @Test(priority = 6, expectedExceptions = {PushProviderServerException.class})
    public void testSendNotificationFailWithInvalidCredentials() throws PushProviderException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_ACCESS_KEY_ID, "");
        when(pushSenderData.getProperties()).thenReturn(properties);

        PushNotificationData notificationData = new PushNotificationData.Builder()
                .setNotificationTitle(TEST_NOTIFICATION_TITLE)
                .setNotificationBody(TEST_NOTIFICATION_BODY)
                .setDeviceHandle(TEST_ENDPOINT_ARN)
                .build();

        snsPushProvider.sendNotification(notificationData, pushSenderData, "carbon.super");
    }

    @Test(priority = 7, expectedExceptions = {PushProviderServerException.class})
    public void testSendNotificationFailWithNullPlatform() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        PushNotificationData notificationData = new PushNotificationData.Builder()
                .setNotificationTitle(TEST_NOTIFICATION_TITLE)
                .setNotificationBody(TEST_NOTIFICATION_BODY)
                .setDeviceHandle("arn:aws:sns:us-east-1:123456789012:endpoint/INVALID/MyApp/12345")
                .build();

        snsPushProvider.sendNotification(notificationData, pushSenderData, "carbon.super");
    }

    @Test(priority = 8, expectedExceptions = {PushProviderException.class})
    public void testSendNotificationFailWithInvalidParameterException() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        try (MockedStatic<NotificationTemplates> mockedTemplates = Mockito.mockStatic(NotificationTemplates.class)) {
            mockedTemplates.when(() -> NotificationTemplates.buildNotificationMessages(
                    anyString(), anyString(), any(Map.class))).thenReturn(TEST_MESSAGE_PAYLOAD);

            PushNotificationData notificationData = new PushNotificationData.Builder()
                    .setNotificationTitle(TEST_NOTIFICATION_TITLE)
                    .setNotificationBody(TEST_NOTIFICATION_BODY)
                    .setDeviceHandle(TEST_ENDPOINT_ARN)
                    .build();

            AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                    .errorMessage("Invalid parameter")
                    .build();
            InvalidParameterException exception = (InvalidParameterException) InvalidParameterException.builder()
                    .awsErrorDetails(errorDetails)
                    .build();
            when(snsClient.publish(any(PublishRequest.class))).thenThrow(exception);

            testProvider.sendNotification(notificationData, pushSenderData, "carbon.super");
        }
    }

    @Test(priority = 9, expectedExceptions = {PushProviderException.class})
    public void testSendNotificationFailWithEndpointNotFound() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        try (MockedStatic<NotificationTemplates> mockedTemplates = Mockito.mockStatic(NotificationTemplates.class)) {
            mockedTemplates.when(() -> NotificationTemplates.buildNotificationMessages(
                    anyString(), anyString(), any(Map.class))).thenReturn(TEST_MESSAGE_PAYLOAD);

            PushNotificationData notificationData = new PushNotificationData.Builder()
                    .setNotificationTitle(TEST_NOTIFICATION_TITLE)
                    .setNotificationBody(TEST_NOTIFICATION_BODY)
                    .setDeviceHandle(TEST_ENDPOINT_ARN)
                    .build();

            AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                    .errorMessage("TargetArn reason: No endpoint found for the target arn specified")
                    .build();
            InvalidParameterException exception = (InvalidParameterException) InvalidParameterException.builder()
                    .awsErrorDetails(errorDetails)
                    .build();
            when(snsClient.publish(any(PublishRequest.class))).thenThrow(exception);

            testProvider.sendNotification(notificationData, pushSenderData, "carbon.super");
        }
    }

    @Test(priority = 10, expectedExceptions = {PushProviderException.class})
    public void testSendNotificationFailWithNotFoundException() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        try (MockedStatic<NotificationTemplates> mockedTemplates = Mockito.mockStatic(NotificationTemplates.class)) {
            mockedTemplates.when(() -> NotificationTemplates.buildNotificationMessages(
                    anyString(), anyString(), any(Map.class))).thenReturn(TEST_MESSAGE_PAYLOAD);

            PushNotificationData notificationData = new PushNotificationData.Builder()
                    .setNotificationTitle(TEST_NOTIFICATION_TITLE)
                    .setNotificationBody(TEST_NOTIFICATION_BODY)
                    .setDeviceHandle(TEST_ENDPOINT_ARN)
                    .build();

            AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                    .errorMessage("Resource not found")
                    .build();
            NotFoundException exception = (NotFoundException) NotFoundException.builder()
                    .awsErrorDetails(errorDetails)
                    .build();
            when(snsClient.publish(any(PublishRequest.class))).thenThrow(exception);

            testProvider.sendNotification(notificationData, pushSenderData, "carbon.super");
        }
    }

    @Test(priority = 11, expectedExceptions = {PushProviderException.class})
    public void testSendNotificationFailWithAuthorizationErrorException() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        try (MockedStatic<NotificationTemplates> mockedTemplates = Mockito.mockStatic(NotificationTemplates.class)) {
            mockedTemplates.when(() -> NotificationTemplates.buildNotificationMessages(
                    anyString(), anyString(), any(Map.class))).thenReturn(TEST_MESSAGE_PAYLOAD);

            PushNotificationData notificationData = new PushNotificationData.Builder()
                    .setNotificationTitle(TEST_NOTIFICATION_TITLE)
                    .setNotificationBody(TEST_NOTIFICATION_BODY)
                    .setDeviceHandle(TEST_ENDPOINT_ARN)
                    .build();

            AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                    .errorMessage("Authorization error")
                    .build();
            AuthorizationErrorException exception = (AuthorizationErrorException) AuthorizationErrorException.builder()
                    .awsErrorDetails(errorDetails)
                    .build();
            when(snsClient.publish(any(PublishRequest.class))).thenThrow(exception);

            testProvider.sendNotification(notificationData, pushSenderData, "carbon.super");
        }
    }

    @Test(priority = 12, expectedExceptions = {PushProviderException.class})
    public void testSendNotificationFailWithGenericSnsException() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        try (MockedStatic<NotificationTemplates> mockedTemplates = Mockito.mockStatic(NotificationTemplates.class)) {
            mockedTemplates.when(() -> NotificationTemplates.buildNotificationMessages(
                    anyString(), anyString(), any(Map.class))).thenReturn(TEST_MESSAGE_PAYLOAD);

            PushNotificationData notificationData = new PushNotificationData.Builder()
                    .setNotificationTitle(TEST_NOTIFICATION_TITLE)
                    .setNotificationBody(TEST_NOTIFICATION_BODY)
                    .setDeviceHandle(TEST_ENDPOINT_ARN)
                    .build();

            AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                    .errorMessage("Generic SNS error")
                    .build();
            SnsException exception = (SnsException) SnsException.builder()
                    .awsErrorDetails(errorDetails)
                    .build();
            when(snsClient.publish(any(PublishRequest.class))).thenThrow(exception);

            testProvider.sendNotification(notificationData, pushSenderData, "carbon.super");
        }
    }

    // ==================== Test registerDevice() - Success Cases ====================

    @Test(priority = 13)
    public void testRegisterDeviceSuccess() throws PushProviderException {
        Map<String, String> properties = createValidPropertiesWithPlatformArn();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(SNSPushProviderConstants.SNS_METADATA_PLATFORM, "fcm");

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, null, TEST_PROVIDER_ID);
        deviceData.setProviderMetadata(metadata);

        CreatePlatformEndpointResponse response = CreatePlatformEndpointResponse.builder()
                .endpointArn(TEST_ENDPOINT_ARN)
                .build();
        when(snsClient.createPlatformEndpoint(any(CreatePlatformEndpointRequest.class))).thenReturn(response);
        doNothing().when(snsClient).close();

        testProvider.registerDevice(deviceData, pushSenderData);

        Assert.assertEquals(deviceData.getDeviceHandle(), TEST_ENDPOINT_ARN);
        verify(snsClient, times(1)).createPlatformEndpoint(any(CreatePlatformEndpointRequest.class));
    }

    @Test(priority = 14)
    public void testRegisterDeviceSuccessWithBaiduPlatform() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        properties.put(SNSPushProviderConstants.SNS_PLATFORM_ARNS + 
                SNSPushProviderConstants.SNS_PLATFORM_DELIMITER + "baidu", TEST_PLATFORM_ARN_BAIDU);
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(SNSPushProviderConstants.SNS_METADATA_PLATFORM, "baidu");
        metadata.put(SNSPushProviderConstants.SNS_METADATA_BAIDU_USERID, TEST_BAIDU_USER_ID);

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, null, TEST_PROVIDER_ID);
        deviceData.setProviderMetadata(metadata);

        CreatePlatformEndpointResponse response = CreatePlatformEndpointResponse.builder()
                .endpointArn(TEST_ENDPOINT_ARN)
                .build();
        when(snsClient.createPlatformEndpoint(any(CreatePlatformEndpointRequest.class))).thenReturn(response);
        doNothing().when(snsClient).close();

        testProvider.registerDevice(deviceData, pushSenderData);

        Assert.assertEquals(deviceData.getDeviceHandle(), TEST_ENDPOINT_ARN);
        verify(snsClient, times(1)).createPlatformEndpoint(any(CreatePlatformEndpointRequest.class));
    }

    @Test(priority = 15)
    public void testRegisterDeviceWithExistingEndpoint() throws PushProviderException {
        Map<String, String> properties = createValidPropertiesWithPlatformArn();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(SNSPushProviderConstants.SNS_METADATA_PLATFORM, "fcm");

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, null, TEST_PROVIDER_ID);
        deviceData.setProviderMetadata(metadata);

        String existingArn = "arn:aws:sns:us-east-1:123456789012:endpoint/GCM/MyApp/existing-endpoint";
        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorMessage("Endpoint already exists with EndpointArn: " + existingArn)
                .build();
        InvalidParameterException exception = (InvalidParameterException) InvalidParameterException.builder()
                .awsErrorDetails(errorDetails)
                .build();
        when(snsClient.createPlatformEndpoint(any(CreatePlatformEndpointRequest.class))).thenThrow(exception);

        testProvider.registerDevice(deviceData, pushSenderData);

        Assert.assertEquals(deviceData.getDeviceHandle(), existingArn);
    }

    // ==================== Test registerDevice() - Failure Cases ====================

    @Test(priority = 16, expectedExceptions = {PushProviderServerException.class})
    public void testRegisterDeviceFailWithInvalidCredentials() throws PushProviderException {
        Map<String, String> properties = new HashMap<>();
        when(pushSenderData.getProperties()).thenReturn(properties);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(SNSPushProviderConstants.SNS_METADATA_PLATFORM, "fcm");

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, null, TEST_PROVIDER_ID);
        deviceData.setProviderMetadata(metadata);

        snsPushProvider.registerDevice(deviceData, pushSenderData);
    }

    @Test(priority = 17, expectedExceptions = {PushProviderClientException.class})
    public void testRegisterDeviceFailWithMissingMetadata() throws PushProviderException {
        Map<String, String> properties = createValidPropertiesWithPlatformArn();
        when(pushSenderData.getProperties()).thenReturn(properties);

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, null, TEST_PROVIDER_ID);
        deviceData.setProviderMetadata(null);

        snsPushProvider.registerDevice(deviceData, pushSenderData);
    }

    @Test(priority = 18, expectedExceptions = {PushProviderClientException.class})
    public void testRegisterDeviceFailWithMissingPlatform() throws PushProviderException {
        Map<String, String> properties = createValidPropertiesWithPlatformArn();
        when(pushSenderData.getProperties()).thenReturn(properties);

        Map<String, String> metadata = new HashMap<>();

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, null, TEST_PROVIDER_ID);
        deviceData.setProviderMetadata(metadata);

        snsPushProvider.registerDevice(deviceData, pushSenderData);
    }

    @Test(priority = 19, expectedExceptions = {PushProviderClientException.class})
    public void testRegisterDeviceFailWithInvalidPlatform() throws PushProviderException {
        Map<String, String> properties = createValidPropertiesWithPlatformArn();
        when(pushSenderData.getProperties()).thenReturn(properties);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(SNSPushProviderConstants.SNS_METADATA_PLATFORM, "invalid_platform");

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, null, TEST_PROVIDER_ID);
        deviceData.setProviderMetadata(metadata);

        snsPushProvider.registerDevice(deviceData, pushSenderData);
    }

    @Test(priority = 20, expectedExceptions = {PushProviderClientException.class})
    public void testRegisterDeviceFailWithMissingBaiduUserId() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        properties.put(SNSPushProviderConstants.SNS_PLATFORM_ARNS + 
                SNSPushProviderConstants.SNS_PLATFORM_DELIMITER + "baidu", TEST_PLATFORM_ARN_BAIDU);
        when(pushSenderData.getProperties()).thenReturn(properties);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(SNSPushProviderConstants.SNS_METADATA_PLATFORM, "baidu");

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, null, TEST_PROVIDER_ID);
        deviceData.setProviderMetadata(metadata);

        snsPushProvider.registerDevice(deviceData, pushSenderData);
    }

    @Test(priority = 21, expectedExceptions = {PushProviderServerException.class})
    public void testRegisterDeviceFailWithMissingPlatformArn() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(SNSPushProviderConstants.SNS_METADATA_PLATFORM, "fcm");

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, null, TEST_PROVIDER_ID);
        deviceData.setProviderMetadata(metadata);

        snsPushProvider.registerDevice(deviceData, pushSenderData);
    }

    @Test(priority = 22, expectedExceptions = {PushProviderException.class})
    public void testRegisterDeviceFailWithInvalidParameterException() throws PushProviderException {
        Map<String, String> properties = createValidPropertiesWithPlatformArn();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(SNSPushProviderConstants.SNS_METADATA_PLATFORM, "fcm");

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, null, TEST_PROVIDER_ID);
        deviceData.setProviderMetadata(metadata);

        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorMessage("Invalid parameter error")
                .build();
        InvalidParameterException exception =
                (InvalidParameterException) InvalidParameterException.builder()
                .awsErrorDetails(errorDetails)
                .build();
        when(snsClient.createPlatformEndpoint(any(CreatePlatformEndpointRequest.class)))
                .thenThrow(exception);

        testProvider.registerDevice(deviceData, pushSenderData);
    }

    @Test(priority = 23, expectedExceptions = {PushProviderException.class})
    public void testRegisterDeviceFailWithExistingEndpointButNoArn() throws PushProviderException {
        Map<String, String> properties = createValidPropertiesWithPlatformArn();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(SNSPushProviderConstants.SNS_METADATA_PLATFORM, "fcm");

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, null, TEST_PROVIDER_ID);
        deviceData.setProviderMetadata(metadata);

        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorMessage("Endpoint already exists without ARN info")
                .build();
        InvalidParameterException exception =
                (InvalidParameterException) InvalidParameterException.builder()
                .awsErrorDetails(errorDetails)
                .build();
        when(snsClient.createPlatformEndpoint(any(CreatePlatformEndpointRequest.class)))
                .thenThrow(exception);

        testProvider.registerDevice(deviceData, pushSenderData);
    }

    @Test(priority = 24, expectedExceptions = {PushProviderException.class})
    public void testRegisterDeviceFailWithNotFoundException() throws PushProviderException {
        Map<String, String> properties = createValidPropertiesWithPlatformArn();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(SNSPushProviderConstants.SNS_METADATA_PLATFORM, "fcm");

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, null, TEST_PROVIDER_ID);
        deviceData.setProviderMetadata(metadata);

        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorMessage("Platform application not found")
                .build();
        NotFoundException exception = (NotFoundException) NotFoundException.builder()
                .awsErrorDetails(errorDetails)
                .build();
        when(snsClient.createPlatformEndpoint(any(CreatePlatformEndpointRequest.class)))
                .thenThrow(exception);

        testProvider.registerDevice(deviceData, pushSenderData);
    }

    @Test(priority = 25, expectedExceptions = {PushProviderException.class})
    public void testRegisterDeviceFailWithSnsException() throws PushProviderException {
        Map<String, String> properties = createValidPropertiesWithPlatformArn();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        Map<String, String> metadata = new HashMap<>();
        metadata.put(SNSPushProviderConstants.SNS_METADATA_PLATFORM, "fcm");

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, null, TEST_PROVIDER_ID);
        deviceData.setProviderMetadata(metadata);

        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorMessage("Generic SNS error")
                .build();
        SnsException exception = (SnsException) SnsException.builder()
                .awsErrorDetails(errorDetails)
                .build();
        when(snsClient.createPlatformEndpoint(any(CreatePlatformEndpointRequest.class)))
                .thenThrow(exception);

        testProvider.registerDevice(deviceData, pushSenderData);
    }

    // ==================== Test unregisterDevice() - Success Cases ====================

    @Test(priority = 26)
    public void testUnregisterDeviceSuccess() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, TEST_ENDPOINT_ARN, TEST_PROVIDER_ID);

        DeleteEndpointResponse response = DeleteEndpointResponse.builder().build();
        when(snsClient.deleteEndpoint(any(Consumer.class))).thenReturn(response);
        doNothing().when(snsClient).close();

        testProvider.unregisterDevice(deviceData, pushSenderData);

        verify(snsClient, times(1)).deleteEndpoint(any(Consumer.class));
    }

    @Test(priority = 27)
    public void testUnregisterDeviceWithBlankDeviceHandle() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, "", TEST_PROVIDER_ID);

        snsPushProvider.unregisterDevice(deviceData, pushSenderData);
        // Should complete without exception
    }

    // ==================== Test unregisterDevice() - Failure Cases ====================

    @Test(priority = 28, expectedExceptions = {PushProviderServerException.class})
    public void testUnregisterDeviceFailWithInvalidCredentials() throws PushProviderException {
        Map<String, String> properties = new HashMap<>();
        when(pushSenderData.getProperties()).thenReturn(properties);

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, TEST_ENDPOINT_ARN, TEST_PROVIDER_ID);

        snsPushProvider.unregisterDevice(deviceData, pushSenderData);
    }

    @Test(priority = 29, expectedExceptions = {PushProviderException.class})
    public void testUnregisterDeviceFailWithSnsException() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, TEST_ENDPOINT_ARN, TEST_PROVIDER_ID);

        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorMessage("Generic SNS error")
                .build();
        SnsException exception = (SnsException) SnsException.builder()
                .awsErrorDetails(errorDetails)
                .build();
        when(snsClient.deleteEndpoint(any(Consumer.class))).thenThrow(exception);

        testProvider.unregisterDevice(deviceData, pushSenderData);
    }

    @Test(priority = 30, expectedExceptions = {PushProviderException.class})
    public void testUnregisterDeviceFailWithAuthorizationErrorException() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, TEST_ENDPOINT_ARN, TEST_PROVIDER_ID);

        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorMessage("Authorization error")
                .build();
        AuthorizationErrorException exception = (AuthorizationErrorException) AuthorizationErrorException.builder()
                .awsErrorDetails(errorDetails)
                .build();
        when(snsClient.deleteEndpoint(any(Consumer.class))).thenThrow(exception);

        testProvider.unregisterDevice(deviceData, pushSenderData);
    }

    // ==================== Test updateDevice() - Success Cases ====================

    @Test(priority = 31)
    public void testUpdateDeviceSuccess() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, TEST_ENDPOINT_ARN, TEST_PROVIDER_ID);

        SetEndpointAttributesResponse response = SetEndpointAttributesResponse.builder().build();
        when(snsClient.setEndpointAttributes(any(Consumer.class))).thenReturn(response);
        doNothing().when(snsClient).close();

        testProvider.updateDevice(deviceData, pushSenderData);

        verify(snsClient, times(1)).setEndpointAttributes(any(Consumer.class));
    }

    @Test(priority = 32)
    public void testUpdateDeviceSuccessWithBaiduPlatform() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        String baiduEndpointArn =
                "arn:aws:sns:us-east-1:123456789012:endpoint/BAIDU/MyApp/12345678-1234-1234-1234-123456789012";
        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, baiduEndpointArn, TEST_PROVIDER_ID);

        SetEndpointAttributesResponse response = SetEndpointAttributesResponse.builder().build();
        when(snsClient.setEndpointAttributes(any(Consumer.class))).thenReturn(response);
        doNothing().when(snsClient).close();

        testProvider.updateDevice(deviceData, pushSenderData);

        verify(snsClient, times(1)).setEndpointAttributes(any(Consumer.class));
    }

    // ==================== Test updateDevice() - Failure Cases ====================

    @Test(priority = 33, expectedExceptions = {PushProviderServerException.class})
    public void testUpdateDeviceFailWithInvalidCredentials() throws PushProviderException {
        Map<String, String> properties = new HashMap<>();
        when(pushSenderData.getProperties()).thenReturn(properties);

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, TEST_ENDPOINT_ARN, TEST_PROVIDER_ID);

        snsPushProvider.updateDevice(deviceData, pushSenderData);
    }

    @Test(priority = 34, expectedExceptions = {PushProviderServerException.class})
    public void testUpdateDeviceFailWithNullPlatform() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        String invalidArn = "arn:aws:sns:us-east-1:123456789012:endpoint/INVALID/MyApp/12345";
        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, invalidArn, TEST_PROVIDER_ID);

        snsPushProvider.updateDevice(deviceData, pushSenderData);
    }

    @Test(priority = 35, expectedExceptions = {PushProviderException.class})
    public void testUpdateDeviceFailWithNotFoundException() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, TEST_ENDPOINT_ARN, TEST_PROVIDER_ID);

        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorMessage("Endpoint not found")
                .build();
        NotFoundException exception = (NotFoundException) NotFoundException.builder()
                .awsErrorDetails(errorDetails)
                .build();
        when(snsClient.setEndpointAttributes(any(Consumer.class))).thenThrow(exception);

        testProvider.updateDevice(deviceData, pushSenderData);
    }

    @Test(priority = 36, expectedExceptions = {PushProviderException.class})
    public void testUpdateDeviceFailWithSnsException() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        TestableAmazonSNSPushProvider testProvider = new TestableAmazonSNSPushProvider(snsClient);

        PushDeviceData deviceData = new PushDeviceData(TEST_DEVICE_TOKEN, TEST_ENDPOINT_ARN, TEST_PROVIDER_ID);

        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorMessage("Generic SNS error")
                .build();
        SnsException exception = (SnsException) SnsException.builder()
                .awsErrorDetails(errorDetails)
                .build();
        when(snsClient.setEndpointAttributes(any(Consumer.class))).thenThrow(exception);

        testProvider.updateDevice(deviceData, pushSenderData);
    }

    // ==================== Test preProcessProperties() ====================

    @Test(priority = 37)
    public void testPreProcessPropertiesSuccess() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        Map<String, String> processedProperties = snsPushProvider.preProcessProperties(pushSenderData);

        Assert.assertNotNull(processedProperties);
        Assert.assertEquals(processedProperties.get(SNSPushProviderConstants.SNS_ACCESS_KEY_ID), TEST_ACCESS_KEY_ID);
    }

    @Test(priority = 38, expectedExceptions = {PushProviderServerException.class})
    public void testPreProcessPropertiesFail() throws PushProviderException {
        Map<String, String> properties = new HashMap<>();
        when(pushSenderData.getProperties()).thenReturn(properties);

        snsPushProvider.preProcessProperties(pushSenderData);
    }

    @Test(priority = 39, expectedExceptions = {PushProviderServerException.class})
    public void testPreProcessPropertiesFailWithMissingAccessKeyId() throws PushProviderException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, TEST_SECRET_ACCESS_KEY);
        properties.put(SNSPushProviderConstants.SNS_REGION, TEST_REGION);
        when(pushSenderData.getProperties()).thenReturn(properties);

        snsPushProvider.preProcessProperties(pushSenderData);
    }

    @Test(priority = 40, expectedExceptions = {PushProviderServerException.class})
    public void testPreProcessPropertiesFailWithMissingSecretAccessKey() throws PushProviderException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
        properties.put(SNSPushProviderConstants.SNS_REGION, TEST_REGION);
        when(pushSenderData.getProperties()).thenReturn(properties);

        snsPushProvider.preProcessProperties(pushSenderData);
    }

    @Test(priority = 41, expectedExceptions = {PushProviderServerException.class})
    public void testPreProcessPropertiesFailWithMissingRegion() throws PushProviderException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, TEST_SECRET_ACCESS_KEY);
        when(pushSenderData.getProperties()).thenReturn(properties);

        snsPushProvider.preProcessProperties(pushSenderData);
    }

    // ==================== Test postProcessProperties() ====================

    @Test(priority = 42)
    public void testPostProcessPropertiesSuccess() throws PushProviderException {
        Map<String, String> properties = createValidProperties();
        when(pushSenderData.getProperties()).thenReturn(properties);

        Map<String, String> processedProperties = snsPushProvider.postProcessProperties(pushSenderData);

        Assert.assertNotNull(processedProperties);
        Assert.assertEquals(processedProperties, properties);
    }

    // ==================== Test updateCredentials() ====================

    @Test(priority = 43)
    public void testUpdateCredentials() throws PushProviderException {
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        snsPushProvider.updateCredentials(pushSenderData, "carbon.super");
        // Should complete without exception - this is a no-op for SNS
    }

    // ==================== Test storePushProviderSecretProperties() ====================

    @Test(priority = 44)
    public void testStoreNewPushProviderSecretProperties()
            throws PushProviderException, SecretManagementException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, TEST_SECRET_ACCESS_KEY);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        try (MockedStatic<SNSProviderDataHolder> mockedDataHolder = Mockito.mockStatic(SNSProviderDataHolder.class)) {
            SNSProviderDataHolder dataHolder = Mockito.mock(SNSProviderDataHolder.class);
            mockedDataHolder.when(SNSProviderDataHolder::getInstance).thenReturn(dataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(dataHolder.getSecretManager()).thenReturn(secretManager);
            when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(false);

            SecretType secretType = new SecretType();
            secretType.setName(PUSH_PROVIDER_SECRET_TYPE);
            secretType.setId("testSecretTypeId");
            when(secretManager.getSecretType(anyString())).thenReturn(secretType);

            Map<String, String> processedProperties = snsPushProvider.storePushProviderSecretProperties(pushSenderData);

            Assert.assertNotNull(processedProperties);
            Assert.assertEquals(processedProperties.get(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY),
                    SNSPushProviderConstants.SNS_SECRET_REFERENCE);
            verify(secretManager, times(1)).addSecret(anyString(), any());
        }
    }

    @Test(priority = 45)
    public void testUpdateExistingPushProviderSecretProperties()
            throws PushProviderException, SecretManagementException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, TEST_SECRET_ACCESS_KEY);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        try (MockedStatic<SNSProviderDataHolder> mockedDataHolder = Mockito.mockStatic(SNSProviderDataHolder.class)) {
            SNSProviderDataHolder dataHolder = Mockito.mock(SNSProviderDataHolder.class);
            mockedDataHolder.when(SNSProviderDataHolder::getInstance).thenReturn(dataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(dataHolder.getSecretManager()).thenReturn(secretManager);
            when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(true);

            SecretType secretType = new SecretType();
            secretType.setName(PUSH_PROVIDER_SECRET_TYPE);
            secretType.setId("testSecretTypeId");
            when(secretManager.getSecretType(anyString())).thenReturn(secretType);

            Map<String, String> processedProperties = snsPushProvider.storePushProviderSecretProperties(pushSenderData);

            Assert.assertNotNull(processedProperties);
            Assert.assertEquals(processedProperties.get(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY),
                    SNSPushProviderConstants.SNS_SECRET_REFERENCE);
            verify(secretManager, times(1)).updateSecretValue(anyString(), anyString(), anyString());
        }
    }

    @Test(priority = 46, expectedExceptions = {PushProviderServerException.class})
    public void testStorePushProviderSecretPropertiesFailWithMissingSecret() throws PushProviderException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, "");
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        snsPushProvider.storePushProviderSecretProperties(pushSenderData);
    }

    @Test(priority = 47, expectedExceptions = {PushProviderServerException.class})
    public void testStorePushProviderSecretPropertiesFailWithNullSecret() throws PushProviderException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, null);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        snsPushProvider.storePushProviderSecretProperties(pushSenderData);
    }

    @Test(priority = 48, expectedExceptions = {PushProviderException.class})
    public void testStorePushProviderSecretPropertiesFailWithSecretManagementException()
            throws PushProviderException, SecretManagementException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, TEST_SECRET_ACCESS_KEY);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        try (MockedStatic<SNSProviderDataHolder> mockedDataHolder = Mockito.mockStatic(SNSProviderDataHolder.class)) {
            SNSProviderDataHolder dataHolder = Mockito.mock(SNSProviderDataHolder.class);
            mockedDataHolder.when(SNSProviderDataHolder::getInstance).thenReturn(dataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(dataHolder.getSecretManager()).thenReturn(secretManager);
            when(secretManager.isSecretExist(anyString(), anyString()))
                    .thenThrow(new SecretManagementException("Test exception"));

            snsPushProvider.storePushProviderSecretProperties(pushSenderData);
        }
    }

    // ==================== Test retrievePushProviderSecretProperties() ====================

    @Test(priority = 49)
    public void testRetrievePushProviderSecretPropertiesSuccess()
            throws PushProviderException, SecretManagementException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, SNSPushProviderConstants.SNS_SECRET_REFERENCE);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        try (MockedStatic<SNSProviderDataHolder> mockedDataHolder = Mockito.mockStatic(SNSProviderDataHolder.class)) {
            SNSProviderDataHolder dataHolder = Mockito.mock(SNSProviderDataHolder.class);
            mockedDataHolder.when(SNSProviderDataHolder::getInstance).thenReturn(dataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            SecretResolveManager secretResolveManager = Mockito.mock(SecretResolveManager.class);
            when(dataHolder.getSecretManager()).thenReturn(secretManager);
            when(dataHolder.getSecretResolveManager()).thenReturn(secretResolveManager);
            when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(true);

            ResolvedSecret resolvedSecret = new ResolvedSecret();
            resolvedSecret.setResolvedSecretValue(TEST_SECRET_ACCESS_KEY);
            when(secretResolveManager.getResolvedSecret(anyString(), anyString())).thenReturn(resolvedSecret);

            Map<String, String> processedProperties =
                    snsPushProvider.retrievePushProviderSecretProperties(pushSenderData);

            Assert.assertNotNull(processedProperties);
            Assert.assertEquals(processedProperties.get(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY),
                    TEST_SECRET_ACCESS_KEY);
        }
    }

    @Test(priority = 50, expectedExceptions = {PushProviderServerException.class})
    public void testRetrievePushProviderSecretPropertiesFailWithMissingReference() throws PushProviderException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, "");
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        snsPushProvider.retrievePushProviderSecretProperties(pushSenderData);
    }

    @Test(priority = 51, expectedExceptions = {PushProviderServerException.class})
    public void testRetrievePushProviderSecretPropertiesFailWithNullReference() throws PushProviderException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, null);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        snsPushProvider.retrievePushProviderSecretProperties(pushSenderData);
    }

    @Test(priority = 52, expectedExceptions = {PushProviderException.class})
    public void testRetrievePushProviderSecretPropertiesFailWithNonExistentSecret()
            throws PushProviderException, SecretManagementException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, SNSPushProviderConstants.SNS_SECRET_REFERENCE);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        try (MockedStatic<SNSProviderDataHolder> mockedDataHolder = Mockito.mockStatic(SNSProviderDataHolder.class)) {
            SNSProviderDataHolder dataHolder = Mockito.mock(SNSProviderDataHolder.class);
            mockedDataHolder.when(SNSProviderDataHolder::getInstance).thenReturn(dataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(dataHolder.getSecretManager()).thenReturn(secretManager);
            when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(false);

            snsPushProvider.retrievePushProviderSecretProperties(pushSenderData);
        }
    }

    @Test(priority = 53, expectedExceptions = {PushProviderException.class})
    public void testRetrievePushProviderSecretPropertiesFailWithSecretManagementException()
            throws PushProviderException, SecretManagementException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, SNSPushProviderConstants.SNS_SECRET_REFERENCE);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        try (MockedStatic<SNSProviderDataHolder> mockedDataHolder = Mockito.mockStatic(SNSProviderDataHolder.class)) {
            SNSProviderDataHolder dataHolder = Mockito.mock(SNSProviderDataHolder.class);
            mockedDataHolder.when(SNSProviderDataHolder::getInstance).thenReturn(dataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(dataHolder.getSecretManager()).thenReturn(secretManager);
            when(secretManager.isSecretExist(anyString(), anyString()))
                    .thenThrow(new SecretManagementException("Test exception"));

            snsPushProvider.retrievePushProviderSecretProperties(pushSenderData);
        }
    }

    // ==================== Test deletePushProviderSecretProperties() ====================

    @Test(priority = 54)
    public void testDeletePushProviderSecretPropertiesSuccess()
            throws PushProviderException, SecretManagementException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, SNSPushProviderConstants.SNS_SECRET_REFERENCE);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        try (MockedStatic<SNSProviderDataHolder> mockedDataHolder = Mockito.mockStatic(SNSProviderDataHolder.class)) {
            SNSProviderDataHolder dataHolder = Mockito.mock(SNSProviderDataHolder.class);
            mockedDataHolder.when(SNSProviderDataHolder::getInstance).thenReturn(dataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(dataHolder.getSecretManager()).thenReturn(secretManager);
            when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(true);

            snsPushProvider.deletePushProviderSecretProperties(pushSenderData);

            verify(secretManager, times(1)).deleteSecret(anyString(), anyString());
        }
    }

    @Test(priority = 55)
    public void testDeletePushProviderSecretPropertiesWithBlankReference() throws PushProviderException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, "");
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        snsPushProvider.deletePushProviderSecretProperties(pushSenderData);
        // Should complete without exception
    }

    @Test(priority = 56)
    public void testDeletePushProviderSecretPropertiesWithNonExistentSecret()
            throws PushProviderException, SecretManagementException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, SNSPushProviderConstants.SNS_SECRET_REFERENCE);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        try (MockedStatic<SNSProviderDataHolder> mockedDataHolder = Mockito.mockStatic(SNSProviderDataHolder.class)) {
            SNSProviderDataHolder dataHolder = Mockito.mock(SNSProviderDataHolder.class);
            mockedDataHolder.when(SNSProviderDataHolder::getInstance).thenReturn(dataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(dataHolder.getSecretManager()).thenReturn(secretManager);
            when(secretManager.isSecretExist(anyString(), anyString())).thenReturn(false);

            snsPushProvider.deletePushProviderSecretProperties(pushSenderData);
            // Should complete without exception - false positive case
        }
    }

    @Test(priority = 57, expectedExceptions = {PushProviderException.class})
    public void testDeletePushProviderSecretPropertiesFailWithSecretManagementException()
            throws PushProviderException, SecretManagementException {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, SNSPushProviderConstants.SNS_SECRET_REFERENCE);
        when(pushSenderData.getProperties()).thenReturn(properties);
        when(pushSenderData.getProviderId()).thenReturn(TEST_PROVIDER_ID);

        try (MockedStatic<SNSProviderDataHolder> mockedDataHolder = Mockito.mockStatic(SNSProviderDataHolder.class)) {
            SNSProviderDataHolder dataHolder = Mockito.mock(SNSProviderDataHolder.class);
            mockedDataHolder.when(SNSProviderDataHolder::getInstance).thenReturn(dataHolder);

            SecretManager secretManager = Mockito.mock(SecretManager.class);
            when(dataHolder.getSecretManager()).thenReturn(secretManager);
            when(secretManager.isSecretExist(anyString(), anyString()))
                    .thenThrow(new SecretManagementException("Test exception"));

            snsPushProvider.deletePushProviderSecretProperties(pushSenderData);
        }
    }

    // ==================== Helper Methods ====================

    private Map<String, String> createValidProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put(SNSPushProviderConstants.SNS_ACCESS_KEY_ID, TEST_ACCESS_KEY_ID);
        properties.put(SNSPushProviderConstants.SNS_SECRET_ACCESS_KEY, TEST_SECRET_ACCESS_KEY);
        properties.put(SNSPushProviderConstants.SNS_REGION, TEST_REGION);
        return properties;
    }

    private Map<String, String> createValidPropertiesWithPlatformArn() {
        Map<String, String> properties = createValidProperties();
        properties.put(SNSPushProviderConstants.SNS_PLATFORM_ARNS + 
                SNSPushProviderConstants.SNS_PLATFORM_DELIMITER + "fcm", TEST_PLATFORM_ARN_FCM);
        return properties;
    }
}
