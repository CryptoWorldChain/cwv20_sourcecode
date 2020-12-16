
package org.brewchain.cvm.exec;

public class CallCreate {

    final byte[] data;
    final byte[] destination;
    final byte[] value;


    public CallCreate(byte[] data, byte[] destination, byte[] value) {
        this.data = data;
        this.destination = destination;
        this.value = value;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getDestination() {
        return destination;
    }

    public byte[] getValue() {
        return value;
    }
}
