package battlecode.client;

import java.io.EOFException;

public interface ClientProxy extends DebugProxy {

	Object readObject() throws EOFException;

	Object peekObject() throws EOFException;

	boolean isDebuggingAvailable();
}
