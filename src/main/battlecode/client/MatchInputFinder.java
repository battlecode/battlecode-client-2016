package battlecode.client;

import battlecode.server.Server;

public class MatchInputFinder extends battlecode.server.MatchInputFinder {

	public String[][] findMatchInputsRemotely(String host) {
		try {			
			// Attempt to make an RPC call to the remote machine.
			Object result = new RPCClient(host).call("find-match-inputs");
			if (result instanceof String[][])
				return (String[][]) result;
			
		} catch (Exception e) {
			Server.warn("couldn't request teams from remote machine: "
						+ e.getMessage());
		}
		
		// If something went wrong...
		return null;
	}

}