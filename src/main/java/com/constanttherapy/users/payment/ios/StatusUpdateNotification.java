package com.constanttherapy.users.payment.ios;

import java.util.List;

/**
 * This is the structure for Apple's status update notifications (basically webhooks)
 * (fields taken from https://developer.apple.com/library/content/documentation/NetworkingInternet/Conceptual/StoreKitGuide/Chapters/Subscriptions.html#//apple_ref/doc/uid/TP40008267-CH7-SW13)
 *
 * @author ehsan
 */
public class StatusUpdateNotification
{
    /**
     * Specifies whether the notification is for a sandbox or a production environment
     */
    Environment environment;
    enum Environment {
        SANDBOX, PROD
    }

    /**
     * Describes the kind of event that triggered the notification.
     */
    NotificationType notification_type;
    enum NotificationType {
        // Initial purchase of the subscription. Store the latest_receipt on your server as a token to verify the user’s subscription status at any time, by validating it with the App Store.
        INITIAL_BUY,
        // Subscription was canceled by Apple customer support. Check Cancellation Date to know the date and time when the subscription was canceled.
        CANCEL,
        // Automatic renewal was successful for an expired subscription. Check Subscription Expiration Date to determine the next renewal date and time.
        RENEWAL,
        // Customer renewed a subscription interactively after it lapsed, either by using your app’s interface or on the App Store in account settings. Service is made available immediately.
        INTERACTIVE_RENEWAL,
        // Customer changed the plan that takes affect at the next subscription renewal. Current active plan is not affected.
        DID_CHANGE_RENEWAL_PREFERENCE,
    }

    /**
     * This value is the same as the shared secret you POST when validating receipts.
     */
    String password;

    /**
     * This value is the same as the Original Transaction Identifier in the receipt. You can use this value to relate multiple iOS 6-style transaction receipts for an individual customer’s subscription.
     */
    String original_transaction_id;

    /**
     * The time and date that a transaction was cancelled by Apple customer support. Posted only if the notification_type is CANCEL.
     */
    String cancellation_date;

    /**
     * The primary key for identifying a subscription purchase. Posted only if the notification_type is CANCEL.
     */
    String web_order_line_item_id;

    /**
     * The base-64 encoded transaction receipt for the most recent renewal transaction. Posted only if the notification_type is RENEWAL or INTERACTIVE_RENEWAL, and only if the renewal is successful.
     */
    String latest_receipt;

    /**
     * The JSON representation of the receipt for the most recent renewal. Posted only if renewal is successful. Not posted for notification_type CANCEL.
     */
    List<ReceiptWrapper.Receipt.InAppPurchaseReceipt> latest_receipt_info;

    /**
     * The base-64 encoded transaction receipt for the most recent renewal transaction. Posted only if the subscription expired.
     */
    String latest_expired_receipt;

    /**
     * The JSON representation of the receipt for the most recent renewal transaction. Posted only if the notification_type is RENEWAL or CANCEL or if renewal failed and subscription expired.
     */
    List<ReceiptWrapper.Receipt.InAppPurchaseReceipt> latest_expired_receipt_info;

    /**
     * This is the same as the auto renew status in the receipt.
     */
    String auto_renew_status;

    /**
     * The current renewal preference for the auto-renewable subscription. This is the Apple ID of the product.
     */
    String auto_renew_adam_id;

    /**
     * This is the same as the Subscription Auto Renew Preference in the receipt.
     */
    String auto_renew_product_id;

    /**
     * This is the same as the Subscription Expiration Intent in the receipt. Posted only if notification_type is RENEWAL or INTERACTIVE_RENEWAL.
     */
    String expiration_intent;
}
