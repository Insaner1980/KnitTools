package com.finnvek.knittools.billing;

/** Offline response driver for the production BillingManager on Android. */
final class AuditPart15BillingClient extends com.android.billingclient.api.BillingClient {
    java.util.List<com.android.billingclient.api.Purchase> purchases = java.util.Collections.emptyList();
    int queryCode = 0;
    int ackCode = 0;
    int queries = 0;
    int acknowledgements = 0;
    boolean deferQueries = false;
    com.android.billingclient.api.PurchasesResponseListener deferredQuery;
    private com.android.billingclient.api.BillingResult result(int code) {
        return com.android.billingclient.api.BillingResult.newBuilder().setResponseCode(code).build();
    }
    @Override public int getConnectionState() { return 2; }
    @Override public com.android.billingclient.api.BillingResult isFeatureSupported(java.lang.String p0) { throw new UnsupportedOperationException("Unexpected test operation: isFeatureSupported"); }
    @Override public com.android.billingclient.api.BillingResult launchBillingFlow(android.app.Activity p0, com.android.billingclient.api.BillingFlowParams p1) { throw new UnsupportedOperationException("Unexpected test operation: launchBillingFlow"); }
    @Override public com.android.billingclient.api.BillingResult showAlternativeBillingOnlyInformationDialog(android.app.Activity p0, com.android.billingclient.api.AlternativeBillingOnlyInformationDialogListener p1) { throw new UnsupportedOperationException("Unexpected test operation: showAlternativeBillingOnlyInformationDialog"); }
    // BillingClient still requires its three deprecated external-offer methods in concrete test clients.
    @SuppressWarnings("deprecation")
    @Override public com.android.billingclient.api.BillingResult showExternalOfferInformationDialog(android.app.Activity p0, com.android.billingclient.api.ExternalOfferInformationDialogListener p1) { throw new UnsupportedOperationException("Unexpected test operation: showExternalOfferInformationDialog"); }
    @Override public com.android.billingclient.api.BillingResult showInAppMessages(android.app.Activity p0, com.android.billingclient.api.InAppMessageParams p1, com.android.billingclient.api.InAppMessageResponseListener p2) { throw new UnsupportedOperationException("Unexpected test operation: showInAppMessages"); }
    @Override public void acknowledgePurchase(com.android.billingclient.api.AcknowledgePurchaseParams p0, com.android.billingclient.api.AcknowledgePurchaseResponseListener p1) { acknowledgements++; p1.onAcknowledgePurchaseResponse(result(ackCode)); }
    @Override public void consumeAsync(com.android.billingclient.api.ConsumeParams p0, com.android.billingclient.api.ConsumeResponseListener p1) { throw new UnsupportedOperationException("Unexpected test operation: consumeAsync"); }
    @Override public void createAlternativeBillingOnlyReportingDetailsAsync(com.android.billingclient.api.AlternativeBillingOnlyReportingDetailsListener p0) { throw new UnsupportedOperationException("Unexpected test operation: createAlternativeBillingOnlyReportingDetailsAsync"); }
    @Override public void createBillingProgramReportingDetailsAsync(com.android.billingclient.api.BillingProgramReportingDetailsParams p0, com.android.billingclient.api.BillingProgramReportingDetailsListener p1) { throw new UnsupportedOperationException("Unexpected test operation: createBillingProgramReportingDetailsAsync"); }
    @SuppressWarnings("deprecation")
    @Override public void createExternalOfferReportingDetailsAsync(com.android.billingclient.api.ExternalOfferReportingDetailsListener p0) { throw new UnsupportedOperationException("Unexpected test operation: createExternalOfferReportingDetailsAsync"); }
    @Override public void endConnection() {
        // This offline response driver never opens a billing service connection.
    }
    @Override public void getBillingChoiceInfoAsync(com.android.billingclient.api.GetBillingChoiceInfoParams p0, com.android.billingclient.api.BillingChoiceInfoResponseListener p1) { throw new UnsupportedOperationException("Unexpected test operation: getBillingChoiceInfoAsync"); }
    @Override public void getBillingConfigAsync(com.android.billingclient.api.GetBillingConfigParams p0, com.android.billingclient.api.BillingConfigResponseListener p1) { throw new UnsupportedOperationException("Unexpected test operation: getBillingConfigAsync"); }
    @Override public void isAlternativeBillingOnlyAvailableAsync(com.android.billingclient.api.AlternativeBillingOnlyAvailabilityListener p0) { throw new UnsupportedOperationException("Unexpected test operation: isAlternativeBillingOnlyAvailableAsync"); }
    @Override public void isBillingProgramAvailableAsync(int p0, com.android.billingclient.api.BillingProgramAvailabilityListener p1) { throw new UnsupportedOperationException("Unexpected test operation: isBillingProgramAvailableAsync"); }
    @SuppressWarnings("deprecation")
    @Override public void isExternalOfferAvailableAsync(com.android.billingclient.api.ExternalOfferAvailabilityListener p0) { throw new UnsupportedOperationException("Unexpected test operation: isExternalOfferAvailableAsync"); }
    @Override public void launchExternalLink(android.app.Activity p0, com.android.billingclient.api.LaunchExternalLinkParams p1, com.android.billingclient.api.LaunchExternalLinkResponseListener p2) { throw new UnsupportedOperationException("Unexpected test operation: launchExternalLink"); }
    @Override public void queryProductDetailsAsync(com.android.billingclient.api.QueryProductDetailsParams p0, com.android.billingclient.api.ProductDetailsResponseListener p1) { throw new UnsupportedOperationException("Unexpected test operation: queryProductDetailsAsync"); }
    @Override public void queryPurchasesAsync(com.android.billingclient.api.QueryPurchasesParams p0, com.android.billingclient.api.PurchasesResponseListener p1) {
        queries++;
        if (deferQueries) {
            deferredQuery = p1;
        } else {
            p1.onQueryPurchasesResponse(result(queryCode), purchases);
        }
    }
    @Override public void showBillingProgramInformationDialog(android.app.Activity p0, com.android.billingclient.api.BillingProgramInformationDialogParams p1, com.android.billingclient.api.BillingProgramInformationDialogListener p2) { throw new UnsupportedOperationException("Unexpected test operation: showBillingProgramInformationDialog"); }
    @Override public void startConnection(com.android.billingclient.api.BillingClientStateListener p0) { throw new UnsupportedOperationException("Unexpected test operation: startConnection"); }
    @Override public boolean isReady() { return true; }
}
