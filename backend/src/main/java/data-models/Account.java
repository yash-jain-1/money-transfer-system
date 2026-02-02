import java.time.Instant;

public class Account {
    double id;
    String holderName;
    private float balance;
    private AccountStatus status; 
    /*
    Instant	- A specific moment on the 
    timeline in UTC (e.g., 2025-11-15T04:00:00Z).
     */
    Instant updatedAt;
    int version;

    public float getBalance(){
        return balance;
    }

    public float debit(float amountToBeDebited){

        newBalance = balance - amountToBeDebited;
        if(newBalance < 100){
            throw NotEnoughBalanceException("Amount could not be debited, balance cannot be less than $100");
        }
        balance = newBalance;
        return balance;
    }
}

public enum AccountStatus{
    ACTIVE,
    LOCKED,
    CLOSED
}



class NotEnoughBalanceException extends Exception{
    public NotEnoughBalanceException(String message){
        super(message);
    }
}