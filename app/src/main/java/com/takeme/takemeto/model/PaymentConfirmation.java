package com.takeme.takemeto.model;

/**
 * Result of a payment attempt via Peach Payments (req 6.2.1, 6.3.1).
 */
public class PaymentConfirmation {

    private String transactionId;
    private boolean success;
    private String failureReason;

    /** No-arg constructor required for Firebase deserialization. */
    public PaymentConfirmation() {}

    public PaymentConfirmation(String transactionId, boolean success, String failureReason) {
        this.transactionId = transactionId;
        this.success = success;
        this.failureReason = failureReason;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
