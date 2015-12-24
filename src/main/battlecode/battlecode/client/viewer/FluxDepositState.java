package battlecode.client.viewer;

import battlecode.common.MapLocation;

/**
 * Class to maintain the drawing state of the flux deposits on the map.
 */
public class FluxDepositState {
    private int depositID;
    private MapLocation location;
    private int roundsAvailable;

    /**
     * Creates a new object to maintain the display state of a flux deposit.
     *
     * @param depositID       the unique identifier of the deposit
     * @param team            the team that owns the flux deposit
     * @param location        the world location of the map deposit
     * @param roundsAvailable the number of available rounds in the flux
     *                           deposit
     */
    public FluxDepositState(int depositID, MapLocation location, int
            roundsAvailable) {
        this.depositID = depositID;
        this.location = location;
        this.roundsAvailable = roundsAvailable;
    }

    /**
     * Copy constructor.
     *
     * @param src the source FluxDepositState to be copied
     */
    public FluxDepositState(FluxDepositState src) {
        this(src.getID(), src.getLocation(), src.getRoundsAvailable());
    }


    /**
     * @return the unique id of the deposit
     */
    public int getID() {
        return depositID;
    }


    /**
     * @return the location of the deposit
     */
    public MapLocation getLocation() {
        return location;
    }

    /**
     * @return the number of rounds available in this deposit
     */
    public int getRoundsAvailable() {
        return roundsAvailable;
    }


    /**
     * Set the new number of rounds available from the deposit.
     *
     * @param roundsAvailable the new count of available rounds
     */
    public void setRoundsAvailable(int roundsAvailable) {
        this.roundsAvailable = roundsAvailable;
    }

}
