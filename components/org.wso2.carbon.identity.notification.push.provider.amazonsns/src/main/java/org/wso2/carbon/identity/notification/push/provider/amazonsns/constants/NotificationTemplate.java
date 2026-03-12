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

import org.json.JSONObject;

import java.util.Map;

/**
 * Notification message templates for different push notification platforms.
 */
public class NotificationTemplate {

    private NotificationTemplate() {

    }

    private static final String DEFAULT_MESSAGE_KEY = "default";

    /**
     * Build notification messages for all platforms in AWS SNS format.
     * Returns a JSON string with platform-specific messages where each platform's payload is a JSON string.
     *
     * @param title           Notification title
     * @param body            Notification body
     * @param additionalData  Additional data to send with the notification
     * @return JSON string in AWS SNS message format
     */
    public static String buildNotificationMessages(String title, String body,
                                                    Map<String, String> additionalData) {

        JSONObject messages = new JSONObject();

        // Default message (required by SNS)
        messages.put(DEFAULT_MESSAGE_KEY, body != null ? body : title);

        // Platform-specific messages (each value must be a JSON string)
        messages.put(SNSPlatformApplication.APNS.getAwsPlatformName(),
                buildAPNSMessage(title, body, additionalData));
        messages.put(SNSPlatformApplication.APNS_SANDBOX.getAwsPlatformName(),
                buildAPNSMessage(title, body, additionalData));
        messages.put(SNSPlatformApplication.FCM.getAwsPlatformName(),
                buildFCMMessage(title, body, additionalData));
        messages.put(SNSPlatformApplication.ADM.getAwsPlatformName(),
                buildADMMessage(title, body, additionalData));
        messages.put(SNSPlatformApplication.BAIDU.getAwsPlatformName(),
                buildBaiduMessage(title, body, additionalData));
        messages.put(SNSPlatformApplication.WNS.getAwsPlatformName(),
                buildWNSMessage(title, body, additionalData));
        messages.put(SNSPlatformApplication.MPNS.getAwsPlatformName(),
                buildMPNSMessage(title, body, additionalData));

        return messages.toString();
    }

    /**
     * Build APNS (Apple Push Notification Service) message.
     * Format: {"aps":{"alert":{"title":"...","body":"..."},"sound":"default"},"customData":{...}}
     *
     * @param title          Notification title.
     * @param body           Notification body.
     * @param additionalData Additional data to send with the notification.
     * @return JSON string in APNS message format.
     */
    private static String buildAPNSMessage(String title, String body, Map<String, String> additionalData) {

        JSONObject aps = new JSONObject();
        JSONObject alert = new JSONObject();

        if (title != null) {
            alert.put("title", title);
        }
        if (body != null) {
            alert.put("body", body);
        }

        aps.put("alert", alert);
        aps.put("sound", "default");

        JSONObject message = new JSONObject();
        message.put("aps", aps);

        // Add additional data if present
        if (additionalData != null && !additionalData.isEmpty()) {
            JSONObject customData = new JSONObject(additionalData);
            message.put("data", customData);
        }

        return message.toString();
    }

    /**
     * Build FCM (Firebase Cloud Messaging) / GCM message.
     * Format: {"notification":{"title":"...","body":"..."},"data":{...}}
     *
     * @param title          Notification title.
     * @param body           Notification body.
     * @param additionalData Additional data to send with the notification.
     * @return JSON string in FCM message format.
     */
    private static String buildFCMMessage(String title, String body, Map<String, String> additionalData) {

        JSONObject notification = new JSONObject();
        if (title != null) {
            notification.put("title", title);
        }
        if (body != null) {
            notification.put("body", body);
        }

        JSONObject message = new JSONObject();
        message.put("notification", notification);

        // Add additional data if present
        if (additionalData != null && !additionalData.isEmpty()) {
            JSONObject data = new JSONObject(additionalData);
            message.put("data", data);
        }

        return message.toString();
    }

    /**
     * Build ADM (Amazon Device Messaging) message.
     * Format: {"data":{"title":"...","message":"...","additionalData":{...}}}
     *
     * @param title          Notification title.
     * @param body           Notification body.
     * @param additionalData Additional data to send with the notification.
     * @return JSON string in ADM message format.
     */
    private static String buildADMMessage(String title, String body, Map<String, String> additionalData) {

        JSONObject data = new JSONObject();
        if (title != null) {
            data.put("title", title);
        }
        if (body != null) {
            data.put("message", body);
        }

        // Add additional data if present
        if (additionalData != null && !additionalData.isEmpty()) {
            for (Map.Entry<String, String> entry : additionalData.entrySet()) {
                data.put(entry.getKey(), entry.getValue());
            }
        }

        JSONObject message = new JSONObject();
        message.put("data", data);

        return message.toString();
    }

    /**
     * Build Baidu Cloud Push message.
     * Format: {"title":"...","description":"...","custom_content":{...}}
     *
     * @param title          Notification title.
     * @param body           Notification body.
     * @param additionalData Additional data to send with the notification.
     * @return JSON string in Baidu message format.
     */
    private static String buildBaiduMessage(String title, String body, Map<String, String> additionalData) {

        JSONObject message = new JSONObject();
        if (title != null) {
            message.put("title", title);
        }
        if (body != null) {
            message.put("description", body);
        }

        // Add additional data if present
        if (additionalData != null && !additionalData.isEmpty()) {
            JSONObject customContent = new JSONObject(additionalData);
            message.put("custom_content", customContent);
        }

        return message.toString();
    }

    /**
     * Build WNS (Windows Push Notification Services) message.
     * Format: Toast notification XML wrapped in JSON.
     *
     * @param title          Notification title.
     * @param body           Notification body.
     * @param additionalData Additional data to send with the notification.
     * @return XML string in WNS toast notification format.
     */
    private static String buildWNSMessage(String title, String body, Map<String, String> additionalData) {

        StringBuilder toastXml = new StringBuilder();
        toastXml.append("<toast><visual><binding template=\"ToastText02\">");

        if (title != null) {
            toastXml.append("<text id=\"1\">").append(escapeXml(title)).append("</text>");
        }
        if (body != null) {
            toastXml.append("<text id=\"2\">").append(escapeXml(body)).append("</text>");
        }

        toastXml.append("</binding></visual></toast>");

        return toastXml.toString();
    }

    /**
     * Build MPNS (Microsoft Push Notification Service) message.
     * Format: Toast notification XML.
     *
     * @param title          Notification title.
     * @param body           Notification body.
     * @param additionalData Additional data to send with the notification.
     * @return XML string in MPNS toast notification format.
     */
    private static String buildMPNSMessage(String title, String body, Map<String, String> additionalData) {

        StringBuilder toastXml = new StringBuilder();
        toastXml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        toastXml.append("<wp:Notification xmlns:wp=\"WPNotification\">");
        toastXml.append("<wp:Toast>");

        if (title != null) {
            toastXml.append("<wp:Text1>").append(escapeXml(title)).append("</wp:Text1>");
        }
        if (body != null) {
            toastXml.append("<wp:Text2>").append(escapeXml(body)).append("</wp:Text2>");
        }

        toastXml.append("</wp:Toast>");
        toastXml.append("</wp:Notification>");

        return toastXml.toString();
    }

    /**
     * Escape XML special characters.
     *
     * @param text Text to escape
     * @return Escaped text
     */
    private static String escapeXml(String text) {

        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
