package bwem.map;

import bwem.typedef.Altitude;
import bwem.Base;
import bwem.typedef.CPPath;
import bwem.ChokePoint;
import bwem.Graph;
import bwem.util.PairGenericAltitudeComparator;
import bwem.util.PairGenericMiniTileAltitudeComparator;
import bwem.util.Timer;
import bwem.area.Area;
import bwem.area.typedef.AreaId;
import bwem.area.TempAreaInfo;
import bwem.check_t;
import bwem.tile.MiniTile;
import bwem.tile.Tile;
import bwem.unit.Geyser;
import bwem.unit.Mineral;
import bwem.unit.Neutral;
import bwem.unit.StaticBuilding;
import bwem.util.BwemExt;
import bwem.util.Utils;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.MutablePair;
import org.openbw.bwapi4j.BW;
import org.openbw.bwapi4j.Player;
import org.openbw.bwapi4j.Position;
import org.openbw.bwapi4j.TilePosition;
import org.openbw.bwapi4j.WalkPosition;
import org.openbw.bwapi4j.unit.Building;
import org.openbw.bwapi4j.unit.MineralPatch;
import org.openbw.bwapi4j.unit.Unit;
import org.openbw.bwapi4j.unit.VespeneGeyser;

public final class MapImpl extends Map {

    private Altitude m_maxAltitude;
    private MutableBoolean m_automaticPathUpdate = new MutableBoolean(false);
    private Graph m_Graph;
    private List<Mineral> m_Minerals = new ArrayList<>();
    private List<Geyser> m_Geysers = new ArrayList<>();
    private List<StaticBuilding> m_StaticBuildings = new ArrayList<>();
    private List<TilePosition> m_StartingLocations = new ArrayList<>();
    private List<MutablePair<MutablePair<AreaId, AreaId>, WalkPosition>> m_RawFrontier = new ArrayList<>();

    public MapImpl(BW bw) {
        super(bw);
        m_Graph = new Graph(this);
    }

//    MapImpl::~MapImpl()
//    {
//        m_automaticPathUpdate = false;		// now there is no need to update the paths
//    }

    @Override
    public void Initialize() {
        Timer overallTimer = new Timer();
        Timer timer = new Timer();

        m_Size = new TilePosition(getBW().getBWMap().mapWidth(), getBW().getBWMap().mapHeight());
        m_size = Size().getX() * Size().getY();
        m_WalkSize = Size().toPosition().toWalkPosition();
        m_walkSize = WalkSize().getX() * WalkSize().getY();
        m_PixelSize = Size().toPosition();
        m_pixelSize = PixelSize().getX() * PixelSize().getY();
        m_center = new Position(PixelSize().getX() / 2, PixelSize().getY() / 2);

        m_Tiles = new ArrayList<>();
        for (int i = 0; i < m_size; ++i) {
            m_Tiles.add(new Tile());
        }

        m_MiniTiles = new ArrayList<>();
        for (int i = 0; i < m_walkSize; ++i) {
            m_MiniTiles.add(new MiniTile());
        }

        m_StartingLocations = new ArrayList<>();
        for (TilePosition t: getBW().getBWMap().getStartPositions()) {
            m_StartingLocations.add(t);
        }

//        JSD_BWEM.serialize_MapImpl_Initialize_MapInfo(m_Size, m_size, m_WalkSize, m_walkSize, m_center, m_StartingLocations);

//    ///	bw << "Map::Initialize-resize: " << timer.ElapsedMilliseconds() << " ms" << endl; timer.Reset();
        System.out.println("Map::Initialize-resize: " + timer.ElapsedMilliseconds() + " ms"); timer.Reset();

        LoadData();
//        JSD_BWEM.serialize_MapImpl_LoadData(m_Tiles, m_MiniTiles);
//    ///	bw << "Map::LoadData: " << timer.ElapsedMilliseconds() << " ms" << endl; timer.Reset();
        System.out.println("Map::LoadData: " + timer.ElapsedMilliseconds() + " ms"); timer.Reset();
//
        DecideSeasOrLakes();
//        JSD_BWEM.serialize_MapImpl_DecideSeasOrLakes(m_Tiles, m_MiniTiles);
//    ///	bw << "Map::DecideSeasOrLakes: " << timer.ElapsedMilliseconds() << " ms" << endl; timer.Reset();
        System.out.println("Map::DecideSeasOrLakes: " + timer.ElapsedMilliseconds() + " ms"); timer.Reset();

        InitializeNeutrals();
//        JSD_BWEM.serialize_MapImpl_InitializeNeutrals(m_Minerals, m_Geysers, m_StaticBuildings);
//    ///	bw << "Map::InitializeNeutrals: " << timer.ElapsedMilliseconds() << " ms" << endl; timer.Reset();
        System.out.println("Map::InitializeNeutrals: " + timer.ElapsedMilliseconds() + " ms"); timer.Reset();

        ComputeAltitude();
//        JSD_BWEM.serialize_MapImpl_ComputeAltitude(m_MiniTiles);
//    ///	bw << "Map::ComputeAltitude: " << timer.ElapsedMilliseconds() << " ms" << endl; timer.Reset();
        System.out.println("Map::ComputeAltitude: " + timer.ElapsedMilliseconds() + " ms"); timer.Reset();

        ProcessBlockingNeutrals();
//        JSD_BWEM.serialize_MapImpl_ProcessBlockingNeutrals(m_Tiles, m_MiniTiles);
//    ///	bw << "Map::ProcessBlockingNeutrals: " << timer.ElapsedMilliseconds() << " ms" << endl; timer.Reset();
        System.out.println("Map::ProcessBlockingNeutrals: " + timer.ElapsedMilliseconds() + " ms"); timer.Reset();

        ComputeAreas();
//    ///	bw << "Map::ComputeAreas: " << timer.ElapsedMilliseconds() << " ms" << endl; timer.Reset();
        System.out.println("Map::ComputeAreas: " + timer.ElapsedMilliseconds() + " ms"); timer.Reset();

        GetGraph().CreateChokePoints();
//    ///	bw << "Graph::CreateChokePoints: " << timer.ElapsedMilliseconds() << " ms" << endl; timer.Reset();
        System.out.println("Map::CreateChokePoints: " + timer.ElapsedMilliseconds() + " ms"); timer.Reset();

        GetGraph().ComputeChokePointDistanceMatrix();
//    ///	bw << "Graph::ComputeChokePointDistanceMatrix: " << timer.ElapsedMilliseconds() << " ms" << endl; timer.Reset();
        System.out.println("Map::ComputeChokePointDistanceMatrix: " + timer.ElapsedMilliseconds() + " ms"); timer.Reset();

        GetGraph().CollectInformation();
//    ///	bw << "Graph::CollectInformation: " << timer.ElapsedMilliseconds() << " ms" << endl; timer.Reset();
        System.out.println("Map::CollectInformation: " + timer.ElapsedMilliseconds() + " ms"); timer.Reset();

        GetGraph().CreateBases();
//    ///	bw << "Graph::CreateBases: " << timer.ElapsedMilliseconds() << " ms" << endl; timer.Reset();
        System.out.println("Map::CreateBases: " + timer.ElapsedMilliseconds() + " ms"); timer.Reset();

//    ///	bw << "Map::Initialize: " << overallTimer.ElapsedMilliseconds() << " ms" << endl;
        System.out.println("Map::Initialize: " + overallTimer.ElapsedMilliseconds() + " ms"); timer.Reset();
    }

    public Graph GetGraph() {
        return m_Graph;
    }

    @Override
    public Altitude MaxAltitude() {
        return m_maxAltitude;
    }

    @Override
    public List<MutablePair<MutablePair<AreaId, AreaId>, WalkPosition>> RawFrontier() {
        return m_RawFrontier;
    }

    @Override
    public MutableBoolean AutomaticPathUpdate() {
        return m_automaticPathUpdate;
    }

    @Override
    public void EnableAutomaticPathAnalysis() {
        m_automaticPathUpdate.setTrue();
    }

    @Override
    public boolean FindBasesForStartingLocations() {
        boolean atLeastOneFailed = false;
        for (TilePosition location : StartingLocations()) {
            boolean found = false;
            for (Area area : GetGraph().Areas()) {
                if (!found) {
                    for (Base base : area.Bases()) {
                        if (!found) {
                            if (BwemExt.queenWiseDist(base.Location(), location) <= BwemExt.max_tiles_between_StartingLocation_and_its_AssignedBase) {
                                base.SetStartingLocation(location);
                                found = true;
                            }
                        }
                    }
                }
            }

            if (!found) {
                atLeastOneFailed = true;
            }
        }

        return !atLeastOneFailed;
    }

    @Override
    public int BaseCount() {
        return GetGraph().BaseCount();
    }

    @Override
    public int ChokePointCount() {
        return GetGraph().ChokePoints().size();
    }

    @Override
    public List<TilePosition> StartingLocations() {
        return m_StartingLocations;
    }

    @Override
    public List<Mineral> Minerals() {
        return m_Minerals;
    }

    @Override
    public List<Geyser> Geysers() {
        return m_Geysers;
    }

    @Override
    public List<StaticBuilding> StaticBuildings() {
        return m_StaticBuildings;
    }

    @Override
    public Mineral GetMineral(Unit u) {
        for (Mineral mineral : m_Minerals) {
            if (mineral.Unit().equals(u)) {
                return mineral;
            }
        }
        return null;
    }

    @Override
    public Geyser GetGeyser(Unit g) {
        for (Geyser geyser : m_Geysers) {
            if (geyser.Unit().equals(g)) {
                return geyser;
            }
        }
        return null;
    }

    @Override
    public void OnUnitDestroyed(Unit u) {
        if (u instanceof MineralPatch) {
            OnMineralDestroyed(u);
        } else {
            try {
                OnStaticBuildingDestroyed(u);
            } catch (Exception ex) {
                //TODO: Handle this exception appropriately.
                /**
                 * An exception WILL be thrown if the unit is not in
                 * the "Map.m_StaticBuildings" list.
                 * Just ignore the exception.
                 */
            }
        }
    }

    @Override
    public void OnMineralDestroyed(Unit u) {
        for (int i = 0; i < m_Minerals.size(); ++i) {
            Mineral mineral = m_Minerals.get(i);
            if (mineral.Unit().equals(u)) {
                OnMineralDestroyed(mineral);
                mineral.simulateCPPObjectDestructor(); /* IMPORTANT! These actions are performed in the "~Neutral" dtor in BWEM 1.4.1 C++. */
                m_Minerals.remove(i--);
                return;
            }
        }
//        bwem_assert(iMineral != m_Minerals.end());
        throw new IllegalArgumentException("Unit is not a Mineral");
    }

    /**
     * This method could be placed in {@link #OnMineralDestroyed(org.openbw.bwapi4j.unit.Unit)}.
     * This remains as a separate method for portability consistency.
     */
    private void OnMineralDestroyed(Mineral pMineral) {
        for (Area area : GetGraph().Areas()) {
            area.OnMineralDestroyed(pMineral);
        }
    }

    @Override
    public void OnStaticBuildingDestroyed(Unit u) {
        for (int i = 0; i < m_StaticBuildings.size(); ++i) {
            StaticBuilding building = m_StaticBuildings.get(i);
            if (building.Unit().equals(u)) {
                building.simulateCPPObjectDestructor(); /* IMPORTANT! These actions are performed in the "~Neutral" dtor in BWEM 1.4.1 C++. */
                m_StaticBuildings.remove(i--);
                return;
            }
        }
//        bwem_assert(iStaticBuilding != m_StaticBuildings.end());
        throw new IllegalArgumentException("Unit is not a StaticBuilding");
    }

    public void OnBlockingNeutralDestroyed(Neutral pBlocking) {
//        bwem_assert(pBlocking && pBlocking->Blocking());
        if (!(pBlocking != null && pBlocking.Blocking())) {
            throw new IllegalArgumentException();
        }

        for (Area pArea : pBlocking.BlockedAreas())
        for (ChokePoint cp : pArea.ChokePoints()) {
            cp.OnBlockingNeutralDestroyed(pBlocking);
        }

        if (GetTile(pBlocking.TopLeft()).GetNeutral() != null) { // there remains some blocking Neutrals at the same location
            return;
        }

        // Unblock the miniTiles of pBlocking:
        AreaId newId = pBlocking.BlockedAreas().iterator().next().Id();
        WalkPosition pBlockingW = pBlocking.Size().toPosition().toWalkPosition();
        for (int dy = 0; dy < pBlockingW.getY(); ++dy)
        for (int dx = 0; dx < pBlockingW.getX(); ++dx) {
            MiniTile miniTile = GetMiniTile_(pBlocking.TopLeft().toPosition().toWalkPosition().add(new WalkPosition(dx, dy)));
            if (miniTile.Walkable()) {
                miniTile.ReplaceBlockedAreaId(newId);
            }
        }

        // Unblock the Tiles of pBlocking:
        for (int dy = 0; dy < pBlocking.Size().getY(); ++dy)
        for (int dx = 0; dx < pBlocking.Size().getX(); ++dx) {
            GetTile_(pBlocking.TopLeft().add(new TilePosition(dx, dy))).ResetAreaId();
            SetAreaIdInTile(pBlocking.TopLeft().add(new TilePosition(dx, dy)));
        }

        if (AutomaticPathUpdate().booleanValue()) {
            GetGraph().ComputeChokePointDistanceMatrix();
        }
    }

    @Override
    public List<Area> Areas() {
        return GetGraph().Areas();
    }

    // Returns an Area given its id. Range = 1..Size()
    @Override
    public Area GetArea(AreaId id) {
        return m_Graph.GetArea(id);
    }

    @Override
    public Area GetArea(WalkPosition w) {
        return m_Graph.GetArea(w);
    }

    @Override
    public Area GetArea(TilePosition t) {
        return m_Graph.GetArea(t);
    }

    @Override
    public Area GetNearestArea(WalkPosition w) {
        return m_Graph.GetNearestArea(w);
    }

    @Override
    public Area GetNearestArea(TilePosition t) {
        return m_Graph.GetNearestArea(t);
    }

    @Override
    public CPPath GetPath(Position a, Position b, MutableInt pLength) {
        return m_Graph.GetPath(a, b, pLength);
    }

    @Override
    public CPPath GetPath(Position a, Position b) {
        return GetPath(a, b, null);
    }

    // Computes walkability, buildability and groundHeight and doodad information, using BWAPI corresponding functions
    private void LoadData() {
    	// Mark unwalkable minitiles (minitiles are walkable by default)
    	for (int y = 0; y < WalkSize().getY(); ++y)
    	for (int x = 0; x < WalkSize().getX(); ++x) {
    		if (!getBW().getBWMap().isWalkable(x, y)) { // For each unwalkable minitile, we also mark its 8 neighbours as not walkable.
    			for (int dy = -1 ; dy <= +1 ; ++dy)     // According to some tests, this prevents from wrongly pretending one Marine can go by some thin path.
    			for (int dx = -1 ; dx <= +1 ; ++dx) {
    				WalkPosition w = new WalkPosition(x + dx, y + dy);
    				if (Valid(w)) {
    					GetMiniTile_(w, check_t.no_check).SetWalkable(false);
                    }
    			}
            }
        }

    	// Mark buildable tiles (tiles are unbuildable by default)
    	for (int y = 0; y < Size().getY(); ++y)
    	for (int x = 0; x < Size().getX(); ++x) {
    		TilePosition t = new TilePosition(x, y);
    		if (getBW().getBWMap().isBuildable(t, false)) {
    			GetTile_(t).SetBuildable();

    			// Ensures buildable ==> walkable:
    			for (int dy = 0 ; dy < 4 ; ++dy)
    			for (int dx = 0 ; dx < 4 ; ++dx) {
    				GetMiniTile_(new WalkPosition(t).add(new WalkPosition(dx, dy)), check_t.no_check).SetWalkable(true);
                }
    		}

    		// Add groundHeight and doodad information:
    		int bwapiGroundHeight = getBW().getBWMap().getGroundHeight(t);
    		GetTile_(t).SetGroundHeight(bwapiGroundHeight / 2);
    		if (bwapiGroundHeight % 2 != 0) {
    			GetTile_(t).SetDoodad();
            }
    	}
    }

    private void DecideSeasOrLakes() {
    	for (int y = 0; y < WalkSize().getY(); ++y)
    	for (int x = 0; x < WalkSize().getX(); ++x) {
    		WalkPosition origin = new WalkPosition(x, y);
    		MiniTile Origin = GetMiniTile_(origin, check_t.no_check);
    		if (Origin.SeaOrLake()) {
    			List<WalkPosition> ToSearch = new ArrayList<>();
                ToSearch.add(origin);
    			List<MiniTile> SeaExtent = new ArrayList<>();
                Origin.SetSea();
                SeaExtent.add(Origin);
    			WalkPosition topLeft = origin;
    			WalkPosition bottomRight = origin;
    			while (!ToSearch.isEmpty()) {
    				WalkPosition current = ToSearch.get(ToSearch.size() - 1);
    				if (current.getX() < topLeft.getX()) topLeft = new WalkPosition(current.getX(), topLeft.getY());
    				if (current.getY() < topLeft.getY()) topLeft = new WalkPosition(topLeft.getX(), current.getY());
    				if (current.getX() > bottomRight.getX()) bottomRight = new WalkPosition(current.getX(), bottomRight.getY());
    				if (current.getY() > bottomRight.getY()) bottomRight = new WalkPosition(bottomRight.getX(), current.getY());

    				ToSearch.remove(ToSearch.size() - 1);
                    WalkPosition deltas[] = {new WalkPosition(0, -1), new WalkPosition(-1, 0), new WalkPosition(+1, 0), new WalkPosition(0, +1)};
    				for (WalkPosition delta : deltas) {
    					WalkPosition next = current.add(delta);
    					if (Valid(next)) {
    						MiniTile Next = GetMiniTile_(next, check_t.no_check);
    						if (Next.SeaOrLake()) {
    							ToSearch.add(next);
    							if (SeaExtent.size() <= BwemExt.lake_max_miniTiles) {
                                    SeaExtent.add(Next);
                                }
    							Next.SetSea();
    						}
    					}
    				}
    			}

    			if ((SeaExtent.size() <= BwemExt.lake_max_miniTiles) &&
    				(bottomRight.getX() - topLeft.getX() <= BwemExt.lake_max_width_in_miniTiles) &&
    				(bottomRight.getY() - topLeft.getY() <= BwemExt.lake_max_width_in_miniTiles) &&
    				(topLeft.getX() >= 2) && (topLeft.getY() >= 2) && (bottomRight.getX() < WalkSize().getX() - 2) && (bottomRight.getY() < WalkSize().getY() - 2)) {
    				for (MiniTile pSea : SeaExtent) {
    					pSea.SetLake();
                    }
                }
    		}
    	}
    }

    private void InitializeNeutrals() {
        for (MineralPatch patch : getBW().getMineralPatches()) {
            m_Minerals.add(new Mineral(patch, this));
        }
        for (VespeneGeyser geyser : getBW().getVespeneGeysers()) {
            m_Geysers.add(new Geyser(geyser, this));
        }
        for (Player player : getBW().getAllPlayers()) {
            if (!player.isNeutral()) {
                continue;
            }
            for (Unit unit : getBW().getUnits(player)) {
                if ((unit instanceof Building) && !(unit instanceof MineralPatch || unit instanceof VespeneGeyser)) {
                    m_StaticBuildings.add(new StaticBuilding(unit, this));
                }
                //TODO: Add "Special_Pit_Door" and "Special_Right_Pit_Door" to static buildings list? See mapImpl.cpp:238.
//				if (n->getType() == Special_Pit_Door)
//					m_StaticBuildings.push_back(make_unique<StaticBuilding>(n, this));
//				if (n->getType() == Special_Right_Pit_Door)
//					m_StaticBuildings.push_back(make_unique<StaticBuilding>(n, this));
            }
        }
    }

    // Assigns MiniTile::m_altitude foar each miniTile having AltitudeMissing()
    // Cf. MiniTile::Altitude() for meaning of altitude_t.
    // Altitudes are computed using the straightforward Dijkstra's algorithm : the lower ones are computed first, starting from the seaside-miniTiles neighbours.
    // The point here is to precompute all possible altitudes for all possible tiles, and sort them.
    private void ComputeAltitude() {
        final int altitude_scale = 8; // 8 provides a pixel definition for altitude_t, since altitudes are computed from miniTiles which are 8x8 pixels

        // 1) Fill in and sort DeltasByAscendingAltitude
        final int range = Math.max(WalkSize().getX(), WalkSize().getY()) / 2 + 3; // should suffice for maps with no Sea.

        List<MutablePair<WalkPosition, Altitude>> DeltasByAscendingAltitude = new ArrayList<>();

        for (int dy = 0; dy <= range; ++dy)
        for (int dx = dy; dx <= range; ++dx) { // Only consider 1/8 of possible deltas. Other ones obtained by symmetry.
            if (dx != 0 || dy != 0) {
                DeltasByAscendingAltitude.add(new MutablePair<>(new WalkPosition(dx, dy), new Altitude((int) (Double.valueOf("0.5") + (Utils.norm(dx, dy) * (double) altitude_scale)))));
            }
        }

//        JSD_BWEM.serialize_MapImpl_ComputeAltitude_DeltasByAscendingAltitude(DeltasByAscendingAltitude);

        Collections.sort(DeltasByAscendingAltitude, new PairGenericAltitudeComparator());

//        JSD_BWEM.serialize_MapImpl_ComputeAltitude_DeltasByAscendingAltitude(DeltasByAscendingAltitude);

        // 2) Fill in ActiveSeaSideList, which basically contains all the seaside miniTiles (from which altitudes are to be computed)
        //    It also includes extra border-miniTiles which are considered as seaside miniTiles too.
        List<MutablePair<WalkPosition, Altitude>> ActiveSeaSideList = new ArrayList<>();

        for (int y = -1; y <= WalkSize().getY(); ++y)
        for (int x = -1; x <= WalkSize().getX(); ++x) {
            WalkPosition w = new WalkPosition(x, y);
            if (!Valid(w) || BwemExt.seaSide(w, this)) {
                ActiveSeaSideList.add(new MutablePair<>(w, new Altitude(0)));
            }
        }

        // 3) Dijkstra's algorithm
        for (MutablePair<WalkPosition, Altitude> delta_altitude : DeltasByAscendingAltitude) {
            final WalkPosition d = new WalkPosition(delta_altitude.left.getX(), delta_altitude.left.getY());
            final Altitude altitude = new Altitude(delta_altitude.right);
            for (int i = 0; i < ActiveSeaSideList.size(); ++i) {
                MutablePair<WalkPosition, Altitude> Current = ActiveSeaSideList.get(i);
                if (altitude.intValue() - Current.right.intValue() >= 2 * altitude_scale) { // optimization : once a seaside miniTile verifies this condition,
                    ActiveSeaSideList.remove(i--);                                           // we can throw it away as it will not generate min altitudes anymore
                } else {
                    WalkPosition[] deltas = {new WalkPosition(d.getX(), d.getY()), new WalkPosition(-d.getX(), d.getY()), new WalkPosition(d.getX(), -d.getY()), new WalkPosition(-d.getX(), -d.getY()),
                                             new WalkPosition(d.getY(), d.getX()), new WalkPosition(-d.getY(), d.getX()), new WalkPosition(d.getY(), -d.getX()), new WalkPosition(-d.getY(), -d.getX())};
                    for (WalkPosition delta : deltas) {
                        WalkPosition w = Current.left.add(delta);
                        if (Valid(w)) {
                            MiniTile miniTile = GetMiniTile_(w, check_t.no_check);
                            if (miniTile.AltitudeMissing()) {
                                m_maxAltitude = new Altitude(altitude);
                                Current.right = new Altitude(altitude);
                                miniTile.SetAltitude(m_maxAltitude);
                            }
                        }
                    }
                }
            }
        }
    }

    private void ProcessBlockingNeutrals() {
        List<Neutral> Candidates = new ArrayList<>();
        for (StaticBuilding s : StaticBuildings()) {
            Candidates.add(s);
        }
        for (Mineral m : Minerals()) {
            Candidates.add(m);
        }

        for (Neutral pCandidate : Candidates) {
            if (pCandidate.NextStacked() == null) { // in the case where several neutrals are stacked, we only consider the top one
                // 1)  Retreave the Border: the outer border of pCandidate
                List<WalkPosition> Border = BwemExt.outerMiniTileBorder(pCandidate.TopLeft(), pCandidate.Size());
                for (int i = 0; i < Border.size(); ++i) {
                    WalkPosition w = Border.get(i);
                    if (!Valid(w) || !GetMiniTile(w, check_t.no_check).Walkable() ||
                            GetTile(w.toPosition().toTilePosition(), check_t.no_check).GetNeutral() != null) {
                        Border.remove(i--);
                    }
                }

                // 2)  Find the doors in Border: one door for each connected set of walkable, neighbouring miniTiles.
                //     The searched connected miniTiles all have to be next to some lake or some static building, though they can't be part of one.
                List<WalkPosition> Doors = new ArrayList<>();
                while (!Border.isEmpty()) {
                    WalkPosition door = Border.remove(Border.size() - 1);
                    Doors.add(door);
                    List<WalkPosition> ToVisit = new ArrayList<>();
                    ToVisit.add(door);
                    List<WalkPosition> Visited = new ArrayList<>();
                    Visited.add(door);
                    while (!ToVisit.isEmpty()) {
                        WalkPosition current = ToVisit.remove(ToVisit.size() - 1);
                        WalkPosition[] deltas = {new WalkPosition(0, -1), new WalkPosition(-1, 0), new WalkPosition(+1, 0), new WalkPosition(0, +1)};
                        for (WalkPosition delta : deltas) {
                            WalkPosition next = current.add(delta);
                            if (Valid(next) && !Visited.contains(next)) {
                                if (GetMiniTile(next, check_t.no_check).Walkable()) {
                                    if (GetTile(next.toPosition().toTilePosition(), check_t.no_check).GetNeutral() == null) {
                                        if (BwemExt.adjoins8SomeLakeOrNeutral(next, this)) {
                                            ToVisit.add(next);
                                            Visited.add(next);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    for (int i = 0; i < Border.size(); ++i) {
                        WalkPosition w = Border.get(i);
                        if (Visited.contains(w)) {
                            Border.remove(i--);
                        }
                    }
                }

                // 3)  If at least 2 doors, find the true doors in Border: a true door is a door that gives onto an area big enough
                List<WalkPosition> TrueDoors = new ArrayList<>();
                if (Doors.size() >= 2)
                    for (WalkPosition door : Doors) {
                        List<WalkPosition> ToVisit = new ArrayList<>();
                        ToVisit.add(door);
                        List<WalkPosition> Visited = new ArrayList<>();
                        Visited.add(door);
                        final int limit = (pCandidate instanceof StaticBuilding) ? 10 : 400;
                        while (!ToVisit.isEmpty() && (Visited.size() < limit)) {
                            WalkPosition current = ToVisit.remove(ToVisit.size() - 1);
                            WalkPosition[] deltas = {new WalkPosition(0, -1), new WalkPosition(-1, 0), new WalkPosition(+1, 0), new WalkPosition(0, +1)};
                            for (WalkPosition delta : deltas) {
                                WalkPosition next = current.add(delta);
                                if (Valid(next) && !Visited.contains(next)) {
                                    if (GetMiniTile(next, check_t.no_check).Walkable()) {
                                        if (GetTile(next.toPosition().toTilePosition(), check_t.no_check).GetNeutral() == null) {
                                            ToVisit.add(next);
                                            Visited.add(next);
                                        }
                                    }
                                }
                            }
                        }
                        if (Visited.size() >= limit) {
                            TrueDoors.add(door);
                        }
                    }

                // 4)  If at least 2 true doors, pCandidate is a blocking static building
                if (TrueDoors.size() >= 2) {
                    // Marks pCandidate (and any Neutral stacked with it) as blocking.
                    for (Neutral pNeutral = GetTile(pCandidate.TopLeft()).GetNeutral(); pNeutral != null; pNeutral = pNeutral.NextStacked()) {
                        pNeutral.SetBlocking(TrueDoors);
                    }

                    // Marks all the miniTiles of pCandidate as blocked.
                    // This way, areas at TrueDoors won't merge together.
                    WalkPosition pCandidateW = pCandidate.Size().toPosition().toWalkPosition();
                    for (int dy = 0; dy < pCandidateW.getY(); ++dy)
                    for (int dx = 0; dx < pCandidateW.getX(); ++dx) {
                        MiniTile miniTile = GetMiniTile_(pCandidate.TopLeft().toPosition().toWalkPosition().add(new WalkPosition(dx, dy)));
                        if (miniTile.Walkable()) {
                            miniTile.SetBlocked();
                        }
                    }
                }
            }
        }
    }

    // Assigns MiniTile::m_areaId for each miniTile having AreaIdMissing()
    // Areas are computed using MiniTile::Altitude() information only.
    // The miniTiles are considered successively in descending order of their Altitude().
    // Each of them either:
    //   - involves the creation of a new area.
    //   - is added to some existing neighbouring area.
    //   - makes two neighbouring areas merge together.
    private void ComputeAreas() {
        List<MutablePair<WalkPosition, MiniTile>> MiniTilesByDescendingAltitude = SortMiniTiles();

        List<TempAreaInfo> TempAreaList = ComputeTempAreas(MiniTilesByDescendingAltitude);

        CreateAreas(TempAreaList);

        SetAreaIdInTiles();
    }

    private List<MutablePair<WalkPosition, MiniTile>> SortMiniTiles() {
        List<MutablePair<WalkPosition, MiniTile>> MiniTilesByDescendingAltitude = new ArrayList<>();
        for (int y = 0; y < WalkSize().getY(); ++y)
        for (int x = 0; x < WalkSize().getX(); ++x) {
            WalkPosition w = new WalkPosition(x, y);
            MiniTile miniTile = GetMiniTile_(w, check_t.no_check);
            if (miniTile.AreaIdMissing()) {
                MiniTilesByDescendingAltitude.add(new MutablePair<>(w, miniTile));
            }
        }

        Collections.sort(MiniTilesByDescendingAltitude, new PairGenericMiniTileAltitudeComparator(PairGenericMiniTileAltitudeComparator.Order.DESCENDING));

        return MiniTilesByDescendingAltitude;
    }

    private List<TempAreaInfo> ComputeTempAreas(List<MutablePair<WalkPosition, MiniTile>> MiniTilesByDescendingAltitude) {
        List<TempAreaInfo> TempAreaList = new ArrayList<>();
        TempAreaList.add(new TempAreaInfo()); // TempAreaList[0] left unused, as AreaIds are > 0
        for (MutablePair<WalkPosition, MiniTile> Current : MiniTilesByDescendingAltitude) {
            final WalkPosition pos = Current.left;
            MiniTile cur = Current.right;

            MutablePair<AreaId, AreaId> neighboringAreas = findNeighboringAreas(pos, this);
            if (neighboringAreas.left == null) { // no neighboring area : creates of a new area
                TempAreaList.add(new TempAreaInfo(new AreaId(TempAreaList.size()), cur, pos));
            } else if (neighboringAreas.right == null) { // one neighboring area : adds cur to the existing area
                TempAreaList.get(neighboringAreas.left.intValue()).Add(cur);
            } else { // two neighboring areas : adds cur to one of them  &  possible merging
                AreaId smaller = new AreaId(neighboringAreas.left);
                AreaId bigger = new AreaId(neighboringAreas.right);
                if (TempAreaList.get(smaller.intValue()).Size() > TempAreaList.get(bigger.intValue()).Size()) {
                    AreaId smaller_tmp = new AreaId(smaller);
                    smaller = new AreaId(bigger);
                    bigger = new AreaId(smaller_tmp);
                }

                // Condition for the neighboring areas to merge:
//                any_of(StartingLocations().begin(), StartingLocations().end(), [&pos](const TilePosition & startingLoc)
//                    { return dist(TilePosition(pos), startingLoc + TilePosition(2, 1)) <= 3;})
                boolean cpp_algorithm_std_any_of = false;
                for (TilePosition startingLoc : StartingLocations()) {
                    if (Double.compare(BwemExt.dist(pos.toPosition().toTilePosition(), startingLoc.add(new TilePosition(2, 1))), Double.valueOf("3")) <= 0) {
                        cpp_algorithm_std_any_of = true;
                        break;
                    }
                }
                if ((TempAreaList.get(smaller.intValue()).Size() < 80)
                        || (TempAreaList.get(smaller.intValue()).HighestAltitude().intValue() < 80)
                        || Double.compare((double) cur.Altitude().intValue() / (double) TempAreaList.get(bigger.intValue()).HighestAltitude().intValue(), Double.valueOf("0.90")) >= 0
                        || Double.compare((double) cur.Altitude().intValue() / (double) TempAreaList.get(smaller.intValue()).HighestAltitude().intValue(), Double.valueOf("0.90")) >= 0
                        || cpp_algorithm_std_any_of) {
                    // adds cur to the absorbing area:
                    TempAreaList.get(bigger.intValue()).Add(cur);

                    // merges the two neighboring areas:
                    ReplaceAreaIds(TempAreaList.get(smaller.intValue()).Top(), bigger);
                    TempAreaList.get(bigger.intValue()).Merge(TempAreaList.get(smaller.intValue()));
                } else { // no merge : cur starts or continues the frontier between the two neighboring areas
                    // adds cur to the chosen Area:
                    TempAreaList.get(chooseNeighboringArea(smaller, bigger).intValue()).Add(cur);
                    m_RawFrontier.add(new MutablePair<>(neighboringAreas, pos));
                }
            }
        }

        // Remove from the frontier obsolete positions
        for (int i = 0; i < m_RawFrontier.size(); ++i) {
            MutablePair<MutablePair<AreaId, AreaId>, WalkPosition> f = m_RawFrontier.get(i);
            if (f.left.left.equals(f.left.right)) {
                m_RawFrontier.remove(i--);
            }
        }

        return TempAreaList;
    }

    private void ReplaceAreaIds(WalkPosition p, AreaId newAreaId) {
        MiniTile Origin = GetMiniTile_(p, check_t.no_check);
        AreaId oldAreaId = Origin.AreaId();
        Origin.ReplaceAreaId(newAreaId);

        List<WalkPosition> ToSearch = new ArrayList<>();
        ToSearch.add(p);
        while (!ToSearch.isEmpty()) {
            WalkPosition current = ToSearch.remove(ToSearch.size() - 1);
            WalkPosition[] deltas = {new WalkPosition(0, -1), new WalkPosition(-1, 0), new WalkPosition(+1, 0), new WalkPosition(0, +1)};
            for (WalkPosition delta : deltas) {
                WalkPosition next = current.add(delta);
                if (Valid(next)) {
                    MiniTile Next = GetMiniTile_(next, check_t.no_check);
                    if (Next.AreaId().equals(oldAreaId)) {
                        ToSearch.add(next);
                        Next.ReplaceAreaId(newAreaId);
                    }
                }
            }
        }

        // also replaces references of oldAreaId by newAreaId in m_RawFrontier:
        if (newAreaId.intValue() > 0) {
            for (MutablePair<MutablePair<AreaId, AreaId>, WalkPosition> f : m_RawFrontier) {
                if (f.left.left.equals(oldAreaId)) {
                    f.left.left = new AreaId(newAreaId);
                }
                if (f.left.right.equals(oldAreaId)) {
                    f.left.right = new AreaId(newAreaId);
                }
            }
        }
    }

    // Initializes m_Graph with the valid and big enough areas in TempAreaList.
    private void CreateAreas(List<TempAreaInfo> TempAreaList) {
        List<MutablePair<WalkPosition, Integer>> AreasList = new ArrayList<>();

        int newAreaId = 1;
        int newTinyAreaId = -2;

        for (TempAreaInfo TempArea : TempAreaList) {
            if (TempArea.Valid()) {
                if (TempArea.Size() >= BwemExt.area_min_miniTiles) {
//                    bwem_assert(newAreaId <= TempArea.Id());
                    if (!(newAreaId <= TempArea.Id().intValue())) {
                        throw new IllegalStateException();
                    }
                    if (newAreaId != TempArea.Id().intValue()) {
                        ReplaceAreaIds(TempArea.Top(), new AreaId(newAreaId));
                    }

                    AreasList.add(new MutablePair<>(TempArea.Top(), TempArea.Size()));
                    ++newAreaId;
                } else {
                    ReplaceAreaIds(TempArea.Top(), new AreaId(newTinyAreaId));
                    --newTinyAreaId;
                }
            }
        }

        GetGraph().CreateAreas(AreasList);
    }

    private void SetAreaIdInTile(TilePosition t) {
        Tile tile = GetTile_(t);
//        bwem_assert(tile.AreaId() == 0);	// initialized to 0
        if (!(tile.AreaId().intValue() == 0)) { // initialized to 0
            throw new IllegalStateException();
        }

        for (int dy = 0; dy < 4; ++dy)
        for (int dx = 0; dx < 4; ++dx) {
            AreaId id = GetMiniTile(t.toPosition().toWalkPosition().add(new WalkPosition(dx, dy)), check_t.no_check).AreaId();
            if (id.intValue() != 0) {
                if (tile.AreaId().intValue() == 0) {
                    tile.SetAreaId(id);
                } else if (!tile.AreaId().equals(id)) {
                    tile.SetAreaId(new AreaId(-1));
                    return;
                }
            }
        }
    }

    private void SetAltitudeInTile(TilePosition t) {
        Altitude minAltitude = new Altitude(Integer.MAX_VALUE);

        for (int dy = 0; dy < 4; ++dy)
        for (int dx = 0; dx < 4; ++dx) {
            Altitude altitude = new Altitude(GetMiniTile(t.toPosition().toWalkPosition().add(new WalkPosition(dx, dy)), check_t.no_check).Altitude());
            if (altitude.intValue() < minAltitude.intValue()) {
                minAltitude = new Altitude(altitude);
            }
        }

        GetTile_(t).SetMinAltitude(minAltitude);
    }

    private void SetAreaIdInTiles() {
        for (int y = 0; y < Size().getY(); ++y)
        for (int x = 0; x < Size().getX(); ++x) {
            TilePosition t = new TilePosition(x, y);
            SetAreaIdInTile(t);
            SetAltitudeInTile(t);
        }
    }

    public static MutablePair<AreaId, AreaId> findNeighboringAreas(WalkPosition p, MapImpl pMap) {
        MutablePair<AreaId, AreaId> result = new MutablePair<>(null, null);

        WalkPosition[] deltas = {new WalkPosition(0, -1), new WalkPosition(-1, 0), new WalkPosition(+1, 0), new WalkPosition(0, +1)};
        for (WalkPosition delta : deltas) {
            if (pMap.Valid(p.add(delta))) {
                AreaId areaId = pMap.GetMiniTile(p.add(delta), check_t.no_check).AreaId();
                if (areaId.intValue() > 0) {
                    if (result.left == null) {
                        result.left = new AreaId(areaId);
                    } else if (!result.left.equals(areaId)) {
                        if (result.right == null || ((areaId.intValue() < result.right.intValue()))) {
                            result.right = new AreaId(areaId);
                        }
                    }
                }
            }
        }

        return result;
    }

    private static AbstractMap<MutablePair<AreaId, AreaId>, Integer> map_AreaPair_counter = new ConcurrentHashMap<>();
    public static AreaId chooseNeighboringArea(AreaId a, AreaId b) {
        if (a.intValue() > b.intValue()) {
            AreaId a_tmp = new AreaId(a);
            a = new AreaId(b);
            b = new AreaId(a_tmp);
        }
        MutablePair<AreaId, AreaId> key = new MutablePair<>(a, b);
        if (map_AreaPair_counter.containsKey(key)) {
            int val = map_AreaPair_counter.get(key);
            map_AreaPair_counter.replace(key, val + 1);
        } else {
            map_AreaPair_counter.put(key, 1);
        }
        return ((map_AreaPair_counter.get(key) - 1) % 2 == 0) ? a : b;
    }

}
