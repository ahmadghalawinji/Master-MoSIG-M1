import java.io.Serializable;

class message implements Serializable {

    private String sourcePhysAddr,
            sourceLogicalAddr,
            destinationPhysAddr,
            destinationLogicalAddr,
            initiator,
            type, message;

    public message(String sourcePhysAddr, String sourceLogicalAddr, String destinationPhysAddr,
            String destinationLogicalAddr, String message, String type) {
        this.sourcePhysAddr = sourcePhysAddr;
        this.sourceLogicalAddr = sourceLogicalAddr;
        this.destinationPhysAddr = destinationPhysAddr;
        this.destinationLogicalAddr = destinationLogicalAddr;
        this.message = message;
        this.type = type;
        initiator = "null";
    }

    public message(String sourcePhysAddr, String sourceLogicalAddr, String destinationPhysAddr,
            String destinationLogicalAddr, String message, String type, String initiator) {
        this.sourcePhysAddr = sourcePhysAddr;
        this.sourceLogicalAddr = sourceLogicalAddr;
        this.destinationPhysAddr = destinationPhysAddr;
        this.destinationLogicalAddr = destinationLogicalAddr;
        this.message = message;
        this.type = type;
        this.initiator = initiator;
    }

    public String getMessage() {
        return message;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    public String getInitiator() {
        return initiator;
    }

    public void setSourceLogicalAdd(String sourceLogicalAddr) {
        this.sourceLogicalAddr = sourceLogicalAddr;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDestinationPhysAdd() {
        return destinationPhysAddr;
    }

    public void setDestinationPhysAdd(String destinationPhysAddr) {
        this.destinationPhysAddr = destinationPhysAddr;
    }

    public String getDestinationLogicAdd() {
        return destinationLogicalAddr;
    }

    public String getSourcePhysAdd() {
        return sourcePhysAddr;
    }

    public void setSourcePhysAdd(String sourcePhysAddr) {
        this.sourcePhysAddr = sourcePhysAddr;
    }

    public String getSourceLogicAdd() {
        return sourceLogicalAddr;
    }

    public String getMsgtype() {
        return type;
    }

}