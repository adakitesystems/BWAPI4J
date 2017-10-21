package bwem;

import bwem.area.Area;
import bwem.map.Map;
import bwem.unit.Geyser;
import bwem.unit.Mineral;
import bwem.unit.Resource;
import java.util.ArrayList;
import java.util.List;
import org.openbw.bwapi4j.Position;
import org.openbw.bwapi4j.TilePosition;
import org.openbw.bwapi4j.type.UnitType;

//////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                          //
//                                  class Base
//                                                                                          //
//////////////////////////////////////////////////////////////////////////////////////////////
//
// After Areas and ChokePoints, Bases are the third kind of object BWEM automatically computes from Brood War's maps.
// A Base is essentially a suggested location (intended to be optimal) to put a Command Center, Nexus, or Hatchery.
// It also provides information on the ressources available, and some statistics.
// A Base alway belongs to some Area. An Area may contain zero, one or several Bases.
// Like Areas and ChokePoints, the number and the addresses of Base instances remain unchanged.
//
// Bases inherit utils::UserData, which provides free-to-use data.
//
//////////////////////////////////////////////////////////////////////////////////////////////

public class Base {

	private Map m_pMap;
	private Area m_pArea;
	private TilePosition m_location;
	private Position m_center;
	private List<Mineral> m_Minerals = new ArrayList<>();
	private List<Geyser> m_Geysers = new ArrayList<>();
	private List<Mineral> m_BlockingMinerals;
	private boolean m_starting = false;

    private Base() {}

    public Base(Area pArea, TilePosition location, List<Resource> AssignedResources, List<Mineral> BlockingMinerals) {
        m_pArea = pArea;
        m_pMap = pArea.GetMap();
        m_location = location;
        m_center = calculateCenter(location);
        m_BlockingMinerals = BlockingMinerals;

//        bwem_assert(!AssignedRessources.empty());
        if (!(!AssignedResources.isEmpty())) {
            throw new IllegalArgumentException("failed assert: !AssignedResources.isEmpty()");
        }

        for (Resource r : AssignedResources) {
            if (r instanceof Mineral) {
                Mineral m = (Mineral) r;
                m_Minerals.add(m);
            } else if (r instanceof Geyser) {
                Geyser g = (Geyser) r;
                m_Geysers.add(g);
            }
        }
    }


//    Base::Base(const Base & Other)
//        : m_pMap(Other.m_pMap), m_pArea(Other.m_pArea)
//    {
//        bwem_assert(false);
//    }


    private Map GetMap() {
        return m_pMap;
    }

	// Tells whether this Base's location is contained in Map::StartingLocations()
	// Note: all players start at locations taken from Map::StartingLocations(),
	//       which doesn't mean all the locations in Map::StartingLocations() are actually used.
	public boolean Starting() {
        return m_starting;
    }

	// Returns the Area this Base belongs to.
	public Area GetArea() {
        return m_pArea;
    }

	// Returns the location of this Base (top left Tile position).
	// If Starting() == true, it is guaranteed that the loction corresponds exactly to one of Map::StartingLocations().
	public TilePosition Location() {
        return m_location;
    }

	// Returns the location of this Base (center in pixels).
	public Position Center() {
        return m_center;
    }

	// Returns the available Minerals.
	// These Minerals are assigned to this Base (it is guaranteed that no other Base provides them).
	// Note: The size of the returned list may decrease, as some of the Minerals may get destroyed.
	public List<Mineral> Minerals() {
        return m_Minerals;
    }

	// Returns the available Geysers.
	// These Geysers are assigned to this Base (it is guaranteed that no other Base provides them).
	// Note: The size of the returned list may NOT decrease, as Geysers never get destroyed.
	public List<Geyser> Geysers() {
        return m_Geysers;
    }

	// Returns the blocking Minerals.
	// These Minerals are special ones: they are placed at the exact location of this Base (or very close),
	// thus blocking the building of a Command Center, Nexus, or Hatchery.
	// So before trying to build this Base, one have to finish gathering these Minerals first.
	// Fortunately, these are guaranteed to have their InitialAmount() <= 8.
	// As an example of blocking Minerals, see the two islands in Andromeda.scx.
	// Note: if Starting() == true, an empty list is returned.
	// Note Base::BlockingMinerals() should not be confused with ChokePoint::BlockingNeutral() and Neutral::Blocking():
	//      the last two refer to a Neutral blocking a ChokePoint, not a Base.
	public List<Mineral> BlockingMinerals() {
        return m_BlockingMinerals;
    }

    public void SetStartingLocation(TilePosition actualLocation) {
        m_starting = true;
        m_location = actualLocation;
        m_center = calculateCenter(actualLocation);
    }

    public void OnMineralDestroyed(Mineral pMineral) {
//    	bwem_assert(pMineral);
        if (!(pMineral != null)) {
            throw new IllegalArgumentException();
        }
        m_Minerals.remove(pMineral);
        m_BlockingMinerals.remove(pMineral);
    }

    private Position calculateCenter(TilePosition location) {
        Position tileSize = UnitType.Terran_Command_Center.tileSize().toPosition();
        int x = location.toPosition().getX();
        int x_offset = tileSize.getX() / 2;
        int y = location.toPosition().getY();
        int y_offset = tileSize.getY() / 2;
        return new Position(x + x_offset, y + y_offset);
    }

}
