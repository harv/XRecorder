package android.os;

/** {@hide} */
interface ICustomService {
    String getCallerName();
    void setCallerName(String callerName);
    String getPhoneNumber();
    void setPhoneNumber(String phoneNumber);
}