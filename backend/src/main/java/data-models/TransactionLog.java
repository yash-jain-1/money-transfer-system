public class TransactionLog {
    double id;
    double fromAccounId;
    double toAccountId;
    float amount;
    TransactionStatus status;
    String failureReason;
    UUID idempotencyKey;
    Instant createdOn;
}

public enum TransactionStatus{
    SUCCESS,
    FAILURE
}