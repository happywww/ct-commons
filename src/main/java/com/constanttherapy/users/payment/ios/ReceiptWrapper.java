package com.constanttherapy.users.payment.ios;

import java.util.List;

/**
 * This is the structure of the receipt returned by Apple's receipt API
 * (fields taken from https://developer.apple.com/library/ios/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html)
 *
 * @author ehsan
 */
public class ReceiptWrapper
{
    /**
     * The status code of the response (0 if the receipt is valid)
     */
    int     status;
    /**
     * The receipt that was sent for verification
     */
    Receipt receipt;
    /**
     * Only returned for iOS 6 style transaction receipts for auto-renewable subscriptions.
     * The JSON representation of the receipt for the most recent renewal.
     */
    List<Receipt.InAppPurchaseReceipt> latest_receipt_info;

    /**
     * Note to developer: Fields that are currently not being used are marked as Deprecated
     *
     * @author ehsan
     */
    class Receipt
    {
        /**
         * The app's bundle identifier.
         * <p/>
         * This corresponds to the value of CFBundleIdentifier in the Info.plist file.
         */
        @Deprecated
        String bundle_id;
        /**
         * The app's version number.
         * <p/>
         * This corresponds to the value of CFBundleVersion (in iOS) or CFBundleShortVersionString (in OS X) in the Info.plist.
         */
        @Deprecated
        String application_version;
        /**
         * The receipt for an in-app purchase.
         * <p/>
         * In the JSON file, the value of this key is an array containing all in-app purchase receipts.
         */
        List<Receipt.InAppPurchaseReceipt> in_app;
        /**
         * The version of the app that was originally purchased.
         * <p/>
         * This corresponds to the value of CFBundleVersion (in iOS) or CFBundleShortVersionString (in OS X) in the Info.plist
         * file when the purchase was originally made.
         * <p/>
         * In the sandbox environment, the value of this field is always 1.0.
         * <p/>
         * Receipts prior to June 20, 2013 omit this field. It is populated on all new receipts, regardless of OS version.
         * If you need the field but it is missing, manually refresh the receipt using the SKReceiptRefreshRequest class.
         */
        @Deprecated
        String original_application_version;
        /**
         * The date that the app receipt expires. (interpreted as an RFC 3339 date)
         * <p/>
         * This key is present only for apps purchased through the Volume Purchase Program. If this key is not present,
         * the receipt does not expire.
         * <p/>
         * When validating a receipt, compare this date to the current date to determine whether the receipt is expired.
         * Do not try to use this date to calculate any other information, such as the time remaining before expiration.
         */
        @Deprecated
        String expiration_date;

        class InAppPurchaseReceipt
        {
            /**
             * The number of items purchased.
             * <p/>
             * This value corresponds to the quantity property of the SKPayment object stored in the transaction’s payment property.
             */
            @Deprecated
            String quantity;
            /**
             * The product identifier of the item that was purchased.
             * <p/>
             * This value corresponds to the productIdentifier property of the SKPayment object stored in the transaction’s payment property.
             */
            String product_id;
            /**
             * The transaction identifier of the item that was purchased.
             * <p/>
             * This value corresponds to the transaction’s transactionIdentifier property.
             */
            String transaction_id;
            /**
             * For a transaction that restores a previous transaction, the transaction identifier of the original transaction.
             * Otherwise, identical to the transaction identifier.
             * <p/>
             * This value corresponds to the original transaction’s transactionIdentifier property.
             * All receipts in a chain of renewals for an auto-renewable subscription have the same value for this field.
             */
            String original_transaction_id;
            /**
             * The date and time that the item was purchased. (interpreted as an RFC 3339 date)
             * <p/>
             * This value corresponds to the transaction’s transactionDate property.
             * For a transaction that restores a previous transaction, the purchase date is the date of the restoration.
             * Use Original Purchase Date to get the date of the original transaction.In an auto-renewable subscription receipt,
             * this is always the date when the subscription was purchased or renewed, regardless of whether the transaction has been restored.
             */
            String purchase_date;
            String purchase_date_ms;
            /**
             * For a transaction that restores a previous transaction, the date of the original transaction. (interpreted as an RFC 3339 date)
             * <p/>
             * This value corresponds to the original transaction’s transactionDate property.
             * In an auto-renewable subscription receipt, this indicates the beginning of the subscription period,
             * even if the subscription has been renewed.
             */
            //String original_purchase_date;
            String original_purchase_date_ms;
            /**
             * The expiration date for the subscription, expressed as the number of milliseconds since January 1, 1970, 00:00:00 GMT.
             * <p/>
             * This key is only present for auto-renewable subscription receipts.
             */
            String expires_date;
            String expires_date_ms;
            /**
             * For a transaction that was canceled by Apple customer support, the time and date of the cancellation.
             * <p/>
             * Treat a canceled receipt the same as if no purchase had ever been made.
             */
            String cancellation_date;
            /**
             * A string that the App Store uses to uniquely identify the application that created the transaction.
             * <p/>
             * If your server supports multiple applications, you can use this value to differentiate between them.
             * Apps are assigned an identifier only in the production environment, so this key is not present for
             * receipts created in the test environment.
             */
            @Deprecated
            String app_item_id;
            /**
             * An arbitrary number that uniquely identifies a revision of your application.
             * <p/>
             * This key is not present for receipts created in the test environment.
             */
            @Deprecated
            String version_external_identifier;
            /**
             * The primary key for identifying subscription purchases.
             */
            @Deprecated
            String web_order_line_item_id;
        }
    }
}
