package battlecode.client;

import java.io.EOFException;

public interface ClientProxy extends DebugProxy {

	public Object readObject() throws EOFException;

	public Object peekObject() throws EOFException;

	public boolean isDebuggingAvailable();
}
