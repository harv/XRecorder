package android.os;

/** {@hide} */
interface ICustomService {
    boolean isBuiltinRecorderExist();
    void setBuiltinRecorderExist(boolean builtinRecorderExist);
    String getCallerName();
    void setCallerName(String callerName);
    String getPhoneNumber();
    void setPhoneNumber(String phoneNumber);
}