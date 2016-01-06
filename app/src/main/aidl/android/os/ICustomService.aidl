package android.os;

/** {@hide} */
interface ICustomService {
    boolean isBuiltinRecorderExist();
    void setBuiltinRecorderExist(boolean builtinRecorderExist);

    boolean isWaitingForRecording();
    void setWaitingForRecording(boolean waitingForRecording);

    boolean isRecordingStopped();
    void setRecordingStopped(boolean recordingStopped);

    String getCallerName();
    void setCallerName(String callerName);

    String getPhoneNumber();
    void setPhoneNumber(String phoneNumber);

    boolean isSetSaveDirectoryable();
    void setSetSaveDirectoryable(boolean setSaveDirectoryable);
}