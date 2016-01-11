package zombomb; //So the program knows what to call our bot

import battlecode.common.*; //imports Battlecode UI
import java.util.Random;	//Use this instead of Math.random(); seeded by the robot's id so more likely to be random than Math.random
import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;
import java.lang.Math;

public class RobotPlayer{
	/**
	 * Useful members
	 *
	 * $rc: the RobotController for this robot. Static so all methods can use it
	 * $randall: our source of all randomness
	 * $ourTeam: our Team, to save bytecodes
	 * $opponentTeam: Opponent's (NOT ZOMBIES) team
	 *
	 * $zombieDenLocations: locations of the zombie dens
	 * $enemyArchonIDs: the IDs of enemy archons
	 */
	static RobotController rc;
	static Random randall;
	static Team ourTeam;
	static Team opponentTeam;
	static int[] zombieRounds;
	static MapLocation[] zombieDenLocations;
	static Collection<Integer> enemyArchonIDs;
	// Collection of <Archon ID, Archon Location, Round that measurement was taken>
	static Collection<Triple<Integer,MapLocation,Integer>> mostRecentEnemyArchonLocations;
	static Direction[] DIRECTIONS = new Direction[]{Direction.NORTH,Direction.NORTH_EAST,Direction.EAST,Direction.SOUTH_EAST,Direction.SOUTH,Direction.SOUTH_WEST,Direction.WEST,Direction.NORTH_WEST};

	/**
	 * run
	 *
	 * @param r: the RobotController passed in by the battlecode software. stored in $rc
	 * @selftype: stores our type so we don't have to call getType() each time, saving bytecodes
	 * @s: the innerclass instance of our robot
	 *
	 */
	public static void run(RobotController r){
		rc = r;
		randall = new Random(rc.getID());
		RobotType selftype = rc.getType();
		ourTeam = rc.getTeam();
		opponentTeam = ourTeam.opponent();
		if(selftype == RobotType.ARCHON){
			Archon s = new RobotPlayer().new Archon();
			s.run();
		}else if(selftype == RobotType.SCOUT){
			Scout s = new RobotPlayer().new Scout();
			s.run();
		}
		else if(selftype == RobotType.GUARD) {
			Guard s = new RobotPlayer().new Guard();
			s.run();
		}
		else if(selftype == RobotType.SOLDIER) {
			Soldier s = new RobotPlayer().new Soldier();
			s.run();
		}
	}
	
	/**
	 * Class Soldier
	 * 
	 * The class outlining our soldier bots
	 * 
	 */
	private class Soldier {
		
		public MapLocation enemyArchonLocation;
		public boolean goOffense;
		public int moveCount;
		
		public Soldier() {
			moveCount = 0;
		}
		
		public void run() {
			while(true) {
				try {
					// Use Guard AI (move out) until there are enough soldiers ammassed around, then go towards enemy archon and attack
					Signal[] signals = rc.emptySignalQueue();
					if (signals.length > 0) {
						for (Signal s : signals) {
							// receive a message containing enemy archon ID
							if (s.getTeam() == ourTeam) {
								FancyMessage f = FancyMessage.getFromRecievedSignal(s);
								if(f.type == 2){
									int xPos = f.ints.first;
									int yPos = f.ints.second;
									enemyArchonLocation = new MapLocation(xPos, yPos);
									goOffense = true;
								}
								/*if (moveCount < 1 && rc.senseRobot(s.getID()).type == RobotType.ARCHON) {
									MapLocation archonLocation = f.senderLocation;
									Direction archonDirection = rc.getLocation().directionTo(archonLocation);
									Direction oppositeDirection = archonDirection.opposite();
									if (rc.isCoreReady() && rc.canMove(oppositeDirection)) {
										moveCount += 1;
										rc.move(oppositeDirection);
									}
								}*/
							}
							// intercept a message containing enemy archon location
							/*if (s.getTeam() == opponentTeam && enemyArchonIDs.contains(s.getID())) {
								FancyMessage f = FancyMessage.getFromRecievedSignal(s);
							}*/
						}
					}
					if(rc.isCoreReady() && goOffense){
						RESOURCE_FUNCTIONS.BUG(enemyArchonLocation);
					}
					if(rc.isWeaponReady()){
					/*	RobotInfo[] robots = rc.senseNearbyRobots();
						for(RobotInfo robot: robots) {
							if((robot.team == Team.ZOMBIE || robot.team == opponentTeam) && rc.canAttackLocation(robot.location)) {
								rc.attackLocation(robot.location);
								break;
							}
						}*/
						RESOURCE_FUNCTIONS.attackWeakestEnemy();
					}
					// If there are enough scouts around, move out towards enemy Archon
					/* (mostRecentEnemyArchonLocations.size() > 0 && RESOURCE_FUNCTIONS.numberOfRobotsInRadiusAndThoseRobots(RobotType.SOLDIER, RobotType.SOLDIER.sensorRadiusSquared, rc.getTeam()).first > 5) {
						RESOURCE_FUNCTIONS.BUG(RESOURCE_FUNCTIONS.mostRecentEnemyArchonLocation());
					}*/
					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/** 
	 * Class Guard
	 * 
	 * The class outlining our Guard bots
	 * 
	 */
	private class Guard {
		
		public MapLocation archonLocation;
		public int moveCount;

		public Guard() {
			moveCount = 0;
		}

		public void run() {
			while(true){
				try{
					archonLocation = RESOURCE_FUNCTIONS.scanFriendlyArchonLocation();
					Signal[] signals = rc.emptySignalQueue();
					if(signals.length > 0){ //if == 0, no signals, so not ready
						for(Signal s: signals){
							if(moveCount < 1 && s.getTeam() == ourTeam && rc.senseRobot(s.getID()).type == RobotType.ARCHON){
								FancyMessage f = FancyMessage.getFromRecievedSignal(s);
								MapLocation archonLocation = f.senderLocation;
								Direction archonDirection = rc.getLocation().directionTo(archonLocation);
								Direction oppositeDirection = archonDirection.opposite();
								if(rc.isCoreReady()){
									if(rc.canMove(oppositeDirection)){
										rc.move(oppositeDirection);
										moveCount += 1;
									}
								}
							}
						}
					}
					if(rc.isCoreReady()){
						RobotInfo[] robots = rc.senseNearbyRobots();
						boolean targetFound = false;
						for(RobotInfo robot:robots){
							if(robot.location.distanceSquaredTo(archonLocation) < 25){
								targetFound = true;
								break;
							}
						}
						if(targetFound == false){
							RESOURCE_FUNCTIONS.BUG(archonLocation);
						}
					}
					if(rc.isWeaponReady()){
						RobotInfo[] robots = rc.senseNearbyRobots();
						/*for(RobotInfo robot: robots) {
							if((robot.team == Team.ZOMBIE) && rc.canAttackLocation(robot.location)) {
								rc.attackLocation(robot.location);
								break;
							}
						}
						for(RobotInfo robot: robots) {
							if((robot.team == opponentTeam) && rc.canAttackLocation(robot.location)) {
								rc.attackLocation(robot.location);
								break;
							}
						}*/
						
						RESOURCE_FUNCTIONS.attackWeakestEnemy();
						//If didn't attack anyone that is adjacent
						if(rc.isWeaponReady()){
							MapLocation target = null;
							for(RobotInfo robot: robots) {
								if((robot.team == Team.ZOMBIE) && robot.location.distanceSquaredTo(archonLocation) < 25) {
									target = robot.location;
									break;
								}
							}
							for(RobotInfo robot: robots) {
								if((robot.team == opponentTeam) && robot.location.distanceSquaredTo(archonLocation) < 25) {
									target = robot.location;
									break;
								}
							}
							if(target != null){
								RESOURCE_FUNCTIONS.BUG(target);
							}
						}
					}
					Clock.yield();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * class Archon
	 *
	 * The class outlining our Archon bots
	 *
	 */
	private class Archon{

		/**
		 * Constructor
		 *
		 */
		public Archon(){
			zombieRounds = rc.getZombieSpawnSchedule().getRounds();
		}

		/**
		 * run
		 *
		 * @while loop: prevents this method from ending. method ending == robot dies D:
		 * @try-catch: catches any errors, prints stack trace, but keeps running.
		 *
		 */
		public void run(){
			while(true){
				try{
					//If it can, always tries to build Scouts.
					if(rc.isCoreReady()){
						RESOURCE_FUNCTIONS.escapeEnemy();
						if(rc.getRoundNum() % 100 == 0){
							FancyMessage.sendMessage(1, 1, 1, 3);
						}
						RobotType type = RESOURCE_FUNCTIONS.chooseRobotType();
						if(RESOURCE_FUNCTIONS.tryBuild(type)){ //See function in RESOURCE_FUNCTIONS to know what it does
							//After building scout, waits a turn, then signals it the location, so it has a good idea of where base is
							Clock.yield();
							FancyMessage.sendMessage(0,0b11101,31,30);
						}
					}
					Clock.yield();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * class Scout
	 *
	 * The class outlining our Scout bots
	 *
	 * @base: where the Archon that spawned it is located. This is how we know where to herd zombies away from
	 * @disciples: the number of zombies that are within sight range and following us.
	 * @last: the last tile the robot was on
	 *
	 */
	private class Scout{
		MapLocation base;
		int disciples;
		MapLocation last;

		/**
		 * Constructor
		 *
		 * initializes disciples value.
		 *
		 */
		public Scout(){
			disciples = 0;
		}

		/**
		 * run
		 *
		 * @while,try: same purpose as Archon.run
		 * @signals: all signals in queue. should be updated to make sure its just from our archons, but basically averages all this stuff out so incase several archons tell it where they are it will avoid all of them
		 * @approxxCoordinates: averages all (assumed) archon coordinates
		 *
		 */
		public void run(){
			while(true){
				try{
					MapLocation enemyArchonLocation = RESOURCE_FUNCTIONS.scanArchonLocation();
					if(enemyArchonLocation != null){
						int xPos = enemyArchonLocation.x;
						int yPos = enemyArchonLocation.y;
						FancyMessage.sendMessage(2, xPos, yPos, 1000);
					}
					//Do nothing until base-line information is gathered: ie, where are the archons. in future, archons may also give message assigning role
					if(base == null){
						Signal[] signals = rc.emptySignalQueue();
						if(signals.length > 0){ //if == 0, no signals, so not ready
							Tuple<Integer,Integer> approxxCoordinates = new Tuple<Integer,Integer>(0,0);
							//averages the signal's origins
							int counted = 0;
							for(int i = 0; i < signals.length; i++){
								if(signals[i].getTeam() == rc.getTeam() && rc.senseRobot(signals[i].getID()).type == RobotType.ARCHON){
									FancyMessage f = FancyMessage.getFromRecievedSignal(signals[i]);
									rc.setIndicatorString(0,"Type:" + f.type + "::Key:" + f.key);
									//rc.setIndicatorString(1,"Info:" + Arrays.toString(f.bits));
									approxxCoordinates.first += f.senderLocation.x;
									approxxCoordinates.second += f.senderLocation.y;
									counted++;
								}
							}
							if(counted > 0){
								approxxCoordinates.first /= counted;
								approxxCoordinates.second /= counted;

								//sets @base to this
								base = new MapLocation(approxxCoordinates.first,approxxCoordinates.second);
								//rc.setIndicatorString(0,"x:" + base.x + "::y:" + base.y);
							}
						}
					}else{
						//If initialized, checks to make sure it isn't losing its herd
						if(stillHerding()){
							//if they're still following, it tries to move
							if(rc.isCoreReady()){
								MapLocation temp = rc.getLocation();
								if(RESOURCE_FUNCTIONS.moveAsFarAwayAsPossibleFrom(base)){//See RESOURCE_FUNCTIONS for details
									last = temp;
								}
							}
							//Otherwise, moves back to where it last was to try to regain them
						}else if(last != null){
							if(rc.isCoreReady()){
								if(rc.canMove(rc.getLocation().directionTo(last))){
									MapLocation temp = rc.getLocation();
									rc.move(temp.directionTo(last));
									last = temp;
								}
							}
						}
					}
					Clock.yield();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}

		/**
		 * boolean stillHerding
		 *
		 * makes sure the bot doesn't lose its herd
		 *
		 * @zombos: array of all zombies within sight
		 * @return true if there's at least 3 zombies, or at least as many as there used to be, false otherwise
		 *
		 */
		public boolean stillHerding(){
			RobotInfo[] zombos = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared,Team.ZOMBIE);
			int count = 0;
			for(int i = 0; i < zombos.length; i++){
				if(zombos[i].type != RobotType.ZOMBIEDEN){
					count++;
				}
			}
			rc.setIndicatorString(1,"disciples:" + disciples + "::count:" + count + "::zombos.length" + zombos.length);
			if(count >= 3 || count >= disciples){
				disciples = count;
				return true;
			}
			return false;
		}
	}

	/**
	 * $class RESOURCE_FUNCTIONS
	 *
	 * Not a class to instantiate, but rather one with some useful functions to use in various robots
	 *
	 */
	public static class RESOURCE_FUNCTIONS{

		/**
		 * Direction intToDir
		 *
		 * Simplifies choosing a random direction
		 *
		 * @param i: the integer to convert
		 * @return: the Direction corresponding to that integer
		 *
		 */
		public static Direction intToDir(int i){
			return DIRECTIONS[(i + 8) % 8];
		}

		/**
		 * MapLocation scanArchonLocation
		 *
		 * @robots: list of all visible enemy robots
		 * @pos: tracks position of Archon in @robots
		 * @return MapLocation of last Archon in list if it exists, null if no Archon is seen.
		 *
		 */
		public static MapLocation scanArchonLocation() {
			RobotInfo[] robots;
			robots = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, opponentTeam);
			for(int i = 0; i < robots.length; i++) {
				if(robots[i].type == RobotType.ARCHON) {
					return robots[i].location;
				}
			}
			return null;
		}
		
		/**
		 * 
		 * MapLocation scanFriendlyArchonLocation
		 * @return location of friendly archon
		 * 
		 */
		public static MapLocation scanFriendlyArchonLocation() {
			RobotInfo[] robots;
			robots = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, ourTeam);
			for(int i = 0; i < robots.length; i++) {
				if(robots[i].type == RobotType.ARCHON) {
					return robots[i].location;
				}
			}
			return null;
		}

		/**
		 * int dirToInt
		 *
		 * Reverses intToDir
		 *
		 * @param d: Direction to convert
		 * @return: the integer corresponding to the Direction
		 *
		 */
		public static int dirToInt(Direction d){
			for(int i = 0; i < DIRECTIONS.length; i++){
				if(d.equals(DIRECTIONS[i])){
					return i;
				}
			}
			return 0;
		}

		/**
		 * boolean tryBuild
		 *
		 * failable build method. Attempts to construct a robot in a space adjacent to the caller
		 *
		 * @param rt: the RobotType of the bot to be built
		 * @param startDirection: optional parameter, inferred to be a random direction if not given. First direction to be checked
		 * @return true if robot is constructed, false otherwise
		 *
		 */
		public static boolean tryBuild(RobotType rt) throws GameActionException{
			return tryBuild(rt,intToDir(randall.nextInt(8)));
		}
		public static boolean tryBuild(RobotType rt,Direction startDirection) throws GameActionException{
			for(int i = 0; i < 8; i++){
				if(rc.canBuild(startDirection,rt)){
					rc.build(startDirection,rt);
					//System.out.println("BUILT!");
					return true;
				}
				startDirection = startDirection.rotateRight();
				//System.out.println(startDirection);
			}
			return false;
		}

		/**
		 * boolean tryAttackLocation
		 * 
		 * failable attack method. Attempts to attack a robot at given map location.
		 * 
		 * @param loc	the location on the map to attack
		 * @return the success of attacking (true if successful, false if unsuccessful)
		 * 
		 */
		public static boolean tryAttackLocation(MapLocation loc) throws GameActionException {
			boolean canAttack = rc.canAttackLocation(loc);
			if (canAttack) {
				rc.attackLocation(loc);
			}
			return canAttack;
		}

		/**
		 * MapLocation findLargestPileOfParts
		 * 
		 * finds the largest pile of parts that is visible to the robot.
		 * 
		 * @return a Tuple containing the location of the largest pile of parts and the size of that pile
		 */
		public static Tuple<MapLocation, Double> findLargestPileOfParts() {
			// create an array of map locations which are visible to the robot
			MapLocation myLocation = rc.getLocation();
			int sensingRadiusSquared = rc.getType().sensorRadiusSquared;
			MapLocation[] visibleLocations = MapLocation.getAllMapLocationsWithinRadiusSq(myLocation, sensingRadiusSquared);

			// find the largest pile of parts
			double maxPileSize = 0;
			MapLocation maxPileLocation = myLocation;
			for (MapLocation loc : visibleLocations) {
				double currentPileSize = rc.senseParts(loc);
				if (currentPileSize > maxPileSize) {
					maxPileSize = currentPileSize;
					maxPileLocation = loc;
				}
			}

			// create Tuple
			Tuple<MapLocation, Double> locationAndSize = new Tuple<MapLocation, Double>(maxPileLocation, maxPileSize);

			return locationAndSize;
		}

		/**
		 * boolean moveAsFarAwayAsPossibleFrom
		 *
		 * Does what it says on the tin. failable. attempts to move to the furthest viable spot from a location
		 *
		 * @param epicenter: the MapLocation to move away from
		 * @current: the current location of the bot
		 * @choices: an array of all possible adjacent choices
		 * @distances: an array of ints that corresponds to @choices, has the squared distance of each choice from @epicenter
		 * @ranked: an array of indexes for @choices, ranked from furthest to closest by a kind of insertion sort
		 * @return true if robot moves, false if not
		 *
		 */
		public static boolean moveAsFarAwayAsPossibleFrom(MapLocation epicenter) throws GameActionException{
			MapLocation current = rc.getLocation();
			MapLocation[] choices = MapLocation.getAllMapLocationsWithinRadiusSq(current,3);
			int[] distances = new int[choices.length];
			int[] ranked = new int[choices.length];
			//populates @distances
			for(int i = 0; i < choices.length;i++){
				distances[i] = choices[i].distanceSquaredTo(epicenter);
			}
			//populates @ranked
			for(int i = 0; i < choices.length; i++){
				int max = -1;
				int accmax = -1;
				//finds largest remaining value in @distances, makes @ranked[i] that index
				for(int j = 0; j < choices.length; j++){
					if(distances[j] > accmax){
						max = j;
						accmax = distances[j];
					}
				}
				ranked[i] = max;
				distances[max] = -2; //prevents chosing the same location several times
			}
			//attempts to move to each space, starting at the furthest, until it does move, upon which it returns true.
			for(int i = 0; i < ranked.length; i++){
				if(rc.canMove(current.directionTo(choices[ranked[i]]))){
					rc.move(current.directionTo(choices[ranked[i]]));
					return true;
				}
			}
			//returns false if it can't move
			//rc.setIndicatorString(1,"max:none");
			return false;
		}

		/**
		 * RobotType chooseRobotType
		 * @param none
		 * @return RobotType that will be produced
		 */
		public static RobotType chooseRobotType() {
			for(int i: zombieRounds){
				int currentRound = rc.getRoundNum();
				if(i-currentRound<=40 && i-currentRound>=0){
					return RobotType.SCOUT;
				}
			}
			if(Math.random()*3>1) {
				return RobotType.SCOUT;
			}
			if(numberOfRobotsInRadiusAndThoseRobots(RobotType.GUARD,3,ourTeam).first == 7){
				return RobotType.SCOUT;
			}
			int fate = randall.nextInt(3);
			if(fate == 0){
				return RobotType.SOLDIER;
			}
			if(fate == 1){
				return RobotType.SCOUT;
			}
			return RobotType.GUARD;
		}

		/**
		 * Returns the number of robots within a given radius squared
		 * @param type the type of robot to look for
		 * @param radiusSqr the squared radius
		 * @param team the team the robot should be on
		 * @return a tuple containing the number of robots nearby and the array of all robots nearby
		 */
		public static Tuple<Integer, RobotInfo[]> numberOfRobotsInRadiusAndThoseRobots(RobotType type,int radiusSqr,Team team){
			int count = 0;
			RobotInfo[] robats = rc.senseNearbyRobots(radiusSqr,team);
			if(type == null){
				Tuple<Integer, RobotInfo[]> thingToReturn = new Tuple<Integer, RobotInfo[]>(robats.length, robats);
				return thingToReturn;
			}
			for(int i = 0; i < robats.length; i++){
				if(robats[i].type.equals(type)){
					count++;
				}
			}
			Tuple<Integer, RobotInfo[]> returnThing = new Tuple<Integer, RobotInfo[]>(count, robats);
			return returnThing;
		}

		/**
		 * Collects the ID of enemy archons within sight range
		 * adds the IDs to the static collection enemyArchonIDs
		 */
		public static void collectEnemyArchonID() {
			Tuple<Integer, RobotInfo[]> robs = numberOfRobotsInRadiusAndThoseRobots(RobotType.ARCHON, rc.getType().sensorRadiusSquared, opponentTeam);
			if (robs.first > 0) {
				for (RobotInfo rob : robs.second) {
					if (!enemyArchonIDs.contains(rob.ID)) {
						enemyArchonIDs.add(rob.ID);
					}
				}
			}
		}
		
		/**
		 * Get most recent enemy archon location
		 */
		public static MapLocation mostRecentEnemyArchonLocation() {
			MapLocation latestArchonLocation = new MapLocation(0,0);
			int latestRound = 0;
			for (Triple<Integer, MapLocation, Integer> trip : mostRecentEnemyArchonLocations) {
				if (trip.third > latestRound) {
					latestRound = trip.third;
					latestArchonLocation = trip.second;
				}
			}
			return latestArchonLocation;
		}

		/**
		 * MapLocation[] inSightButOffMap
		 *
		 * Looks at all tiles in sight range, returns all those that are off the map
		 *
		 * @return array of MapLocations in sight but not on the map
		 *
		 */
		public static MapLocation[] inSightButOffMap() throws GameActionException{
			MapLocation[] allInSight = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(),rc.getType().sensorRadiusSquared);
			int numOffMap = 0;
			for(int i = 0; i < allInSight.length; i++){
				if(rc.onTheMap(allInSight[i])){
					allInSight[i] = null;
				}else{
					numOffMap++;
				}
			}
			MapLocation[] ret = new MapLocation[numOffMap];
			int count = 0;
			for(int i = 0; i < allInSight.length && count < ret.length; i++){
				if(allInSight[i] != null){
					ret[count] = allInSight[i];
					count++;
				}
			}
			return ret;
		}

		/**
		 * boolean BUG
		 *
		 * Pathfinding method. Here's how it basically works:
		 * * * If we can move directly towards target, do that.
		 * * * Otherwise, we move along side of obstacle until we can move directly towards target
		 * * * * * Remember Branches (where we start/stop following walls. If we return to one, obviously following the one way didn't help, so we try the other one. If both are tried, we run the first way again for all following runs)
		 *
		 * @param target: where we are trying to get to
		 * @return true if we move, false if we don't
		 *
		 */
		public static boolean BUG(MapLocation target) throws GameActionException{
			rc.setIndicatorString(1,target.toString());
			if(!rc.isCoreReady()) return false;
			MapLocation current = rc.getLocation();
			Direction directionToTarget = current.directionTo(target);
			if(rc.canMove(directionToTarget) && (Branch.last == null || !Branch.last.contains(current.add(directionToTarget)))){
				rc.setIndicatorString(2,"Starting no branch // ");
				rc.setIndicatorString(0,current.add(directionToTarget).toString() + " from no branch");
				Branch.last = new ArrayList<MapLocation>();
				Branch.last.add(current);
				rc.move(directionToTarget);
				Branch.lastStatus = 0;
				if(current.add(directionToTarget).equals(target)){
					Branch.resetPath();
				}
				return true;
			}else if(Branch.lastStatus == 0){
				rc.setIndicatorString(2,"Starting choosing branch");
				Branch decision = Branch.fork(current,target);
				rc.setIndicatorString(1,"Made it past $fork");
				MapLocation bestChoice = decision.bestBranch();
				rc.setIndicatorString(1,"Made it past @bestBranch");
				if(bestChoice != null){
					rc.setIndicatorString(0,bestChoice.toString() + " from choosing branch");
					Branch.last.add(current);
					rc.move(current.directionTo(bestChoice));
					return true;
				}
				rc.setIndicatorString(0,"Failed in choosing branch");
				return false;
			}else if(Branch.lastStatus == 1){
				rc.setIndicatorString(2,"Starting right branch");
				Branch currentStep = new Branch(current,target);
				Branch temp = Branch.getFromEarlier(currentStep);
				if(temp != null){
					currentStep = temp;
				}
				MapLocation nextChoice = currentStep.getRightCanditate();
				if(nextChoice != null){
					rc.setIndicatorString(0,nextChoice.toString() + " from right branch");
					Branch.last.add(current);
					rc.move(current.directionTo(nextChoice));
					return true;
				}
				rc.setIndicatorString(0,"Failed in right branch");
				return false;
			}else if(Branch.lastStatus == -1){
				rc.setIndicatorString(2,"Starting left branch");
				Branch currentStep = new Branch(current,target);
				Branch temp = Branch.getFromEarlier(currentStep);
				if(temp != null){
					currentStep = temp;

				}
				MapLocation nextChoice = currentStep.getLeftCanditate();
				if(nextChoice != null){
					rc.setIndicatorString(0,nextChoice.toString() + " from left branch");
					Branch.last.add(current);
					rc.move(current.directionTo(nextChoice));
					return true;
				}
				rc.setIndicatorString(0,"Failed in choosing branch");
				return false;
			}
			rc.setIndicatorString(0,"Failed outside of branch");
			return false;
		}
		public static void escapeEnemy(){
			RobotInfo[] enemies = locateEnemy();
			if(enemies == null){
				return;
			}
			ArrayList<RobotInfo> dangerousEnemies = dangerousRobotLocation(enemies);
			if(dangerousEnemies == null){
				return;
			}
			try{
				for(RobotInfo dangerousEnemy: dangerousEnemies){
					Direction escapeDirection = calculateEscapeDirection(dangerousEnemy.location);
					if(rc.canMove(escapeDirection)){
						System.out.println("success");
						rc.move(escapeDirection);
					}
				}
			}
			catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
		}

		public static RobotInfo[] locateEnemy(){
			RobotInfo[] sensedRobots = rc.senseHostileRobots(rc.getLocation(),rc.getType().sensorRadiusSquared);
			if(sensedRobots != null){
				return sensedRobots;
			}
			return null;
		}
		
		public static ArrayList<RobotInfo> dangerousRobotLocation(RobotInfo[] enemies){
			ArrayList<RobotInfo> dangerousEnemies = new ArrayList<RobotInfo>();
			for(RobotInfo enemy: enemies){
				if(enemy.location.distanceSquaredTo(rc.getLocation()) <= enemy.type.attackRadiusSquared){
					dangerousEnemies.add(enemy);
				}
			}
			return dangerousEnemies;
		}

		public static Direction calculateEscapeDirection(MapLocation enemyLocation){
			MapLocation myLocation = rc.getLocation();
			int xDifference = enemyLocation.x - myLocation.x;
			int yDifference = enemyLocation.y - myLocation.y;
			if(xDifference>0 && yDifference>0){
				return Direction.NORTH_WEST;
			}
			else if(xDifference>0 && yDifference<0){
				return Direction.SOUTH_WEST;
			}
			else if(xDifference<0 && yDifference<0){
				return Direction.SOUTH_EAST;
			}
			else if(xDifference<0 && yDifference>0){
				return Direction.NORTH_EAST;
			}
			else if(xDifference>0){
				return Direction.WEST;
			}
			else if(xDifference<0){
				return Direction.EAST;
			}
			else if(yDifference>0){
				return Direction.NORTH;
			}
			return Direction.SOUTH;

		}
		
		public static void attackWeakestEnemy(){
			MapLocation weakestEnemyLocation = locateWeakestEnemy();
			if(weakestEnemyLocation==null){
				return;
			}
			try{
				rc.attackLocation(weakestEnemyLocation);
			}
			catch (Exception e) {
        			System.out.println(e.getMessage());
        		        e.printStackTrace();
        		}

		}

		public static MapLocation locateWeakestEnemy(){
			RobotInfo[] sensedRobots = rc.senseHostileRobots(rc.getLocation(),Math.min(rc.getType().sensorRadiusSquared, rc.getType().attackRadiusSquared));
			RobotInfo weakest=null;
			if(sensedRobots != null){
				for(RobotInfo robot: sensedRobots){
					if(robot.team == opponentTeam || robot.team == Team.ZOMBIE){
						if(weakest == null || robot.health<weakest.health){
							weakest=robot;
						}
					}
				}
				if(weakest!= null){
					return weakest.location;
				}
			}
			return null;
		}

	}

	/**
	 * Tuple
	 * 
	 * a simple tuple class so that tuples can be used.
	 */
	public static class Tuple<X, Y> { 
		public X first; 
		public Y second; 
		public Tuple(X first, Y second) { 
			this.first = first; 
			this.second = second; 
		} 
	}
	
	/**
	 * Triple
	 * 
	 * a simple triple class so that triples can be used.
	 */
	public static class Triple<X, Y, Z> {
		public X first;
		public Y second;
		public Z third;
		public Triple(X first, Y second, Z third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}
	}

	/**
	 * class FancyMessage
	 * 
	 * To SEND a message, prepare data (either as two (24 and 32 bit maximum size respectively) integers or boolean[] of bits (max size 56)) and pass to appropriate @sendMessage with a type and radiusSqr
	 * To RECIEVE a message, pass the recieved signal (ONLY do with a signal from a friendly Archon/Scout) to @getFromRecievedSignal which returns a FancyMessage object, where you can access its various fields
	 *
	 * @senderID: the id of the robot that sent the Signal
	 * @senderLocation: the location of the robot that sent the signal
	 * @bits: an array of bits representing the information sent in the message
	 * @type: the type of the message
	 * @key: the key of the message
	 *
	 */
	public static class FancyMessage{
		public int senderID;
		public MapLocation senderLocation;
		public boolean[] bits;
		public Tuple<Integer,Integer> ints;
		private static Tuple<Integer,Integer> hiddenInts;
		public int type;
		public int key;
		public FancyMessage(){
		}

		/**
		 * FancyMessage getFromRecievedSignal
		 *
		 * Handles recieving of FancyMessages
		 *
		 * @param s: the Signal to decode
		 * @return FancyMessage that was recieved
		 * Also sets @key, @bits, @type, @senderID, @senderLocation, @ints
		 *
		 */
		public static FancyMessage getFromRecievedSignal(Signal s){
			FancyMessage ret = new FancyMessage();
			ret.senderID = s.getID();
			ret.senderLocation = s.getLocation();
			int[] is = s.getMessage();
			Tuple<Integer,boolean[]> info = decrypt(new Tuple<Integer,Integer>(is[0],is[1]));
			ret.type = info.first;
			ret.bits = info.second;
			ret.ints = hiddenInts;
			hiddenInts = null;
			ret.key = (is[0] & 0b11110000) >> 4;
			return ret;
		}

		/**
		 * boolean sendMessage
		 *
		 * OVERLOADED
		 *
		 * handles sending of FancyMessages
		 *
		 * @param type: the type of message being sent (4 bits)
		 * @param data: an array of booleans (only up to 56 elements) to be encoded
		 * @param first,second: the two ints to be encoded (24 bit max for first, 32 for second)
		 * @param encodeddata: for if your data is already encoded before sending
		 * @param radiusSqr: how far to send messsage
		 * @return true unless some sort of failure
		 *
		 */
		public static boolean sendMessage(int type,boolean[] data,int radiusSqr) throws GameActionException{
			Tuple<Integer,Integer> encoded = encrypt(type,data);
			if(encoded == null){
				return false;
			}
			return sendMessage(encoded,radiusSqr);
		}
		public static boolean sendMessage(int type,int first,int second,int radiusSqr) throws GameActionException{
			Tuple<Integer,Integer> encoded = encrypt(type,first,second);
			return sendMessage(encoded,radiusSqr);
		}
		public static boolean sendMessage(Tuple<Integer,Integer> encodeddata, int radiusSqr) throws GameActionException{
			rc.broadcastMessageSignal(encodeddata.first,encodeddata.second,radiusSqr);
			return true;
		}

		/**
		 * Tuple<Integer,boolean[]> decrypt
		 *
		 * Decodes signal to FancyMessage
		 *
		 * @param inputs: the two ints from the message
		 * @return Tuple containing the type of the message and the boolean[] containing its data
		 * also sets @ints
		 *
		 */
		public static Tuple<Integer,boolean[]> decrypt(Tuple<Integer,Integer> inputs){
			int typeIn = inputs.first & 0b1111;
			int keyIn = (inputs.first & 0b11110000) >> 4;
			int encryptor = 0;
			for(int i = 0; i < 8; i++){
				encryptor |= (keyIn & 0b1111) << (i * 4);
			}
			int first = (inputs.first ^ encryptor) >> 8;
				int second = (inputs.second ^ encryptor);
				hiddenInts = new Tuple<Integer,Integer>(first,second);
				boolean[] bit = new boolean[56];
				for(int i = 0; i < 24; i++){
					bit[i] = (first & (1 << i)) != 0;
				}
				for(int i = 0; i < 32; i++){
					bit[i + 24] = (second & (1 << i)) != 0;
				}
				return new Tuple<Integer,boolean[]>(typeIn,bit);
		}

		/**
		 * Tuple<Integer,Integer> encrypt
		 *
		 * OVERLOADED
		 *
		 * encodes data to send
		 *
		 * @param type: the type of message being sent
		 * @param data: array containing bits to be sent
		 * @param first,second: two ints to be sent (24b,32b)
		 * @return Tuple of ints containing encoded version of parameters
		 *
		 */
		public static Tuple<Integer,Integer> encrypt(int type,boolean[] data){
			if(data.length > 56){
				return null;
			}
			int first = 0;
			int second = 0;
			for(int i = 0; i < 24 && i < data.length; i++){
				if(data[i]){
					first |= (1 << i);
				}
			}
			for(int i = 0; i < 32 && i + 24 < data.length; i++){
				if(data[i + 24]){
					second |= (1 << i);
				}
			}
			return encrypt(type,first,second);
		}
		public static Tuple<Integer,Integer> encrypt(int type,int first,int second){
			int key = randall.nextInt(0b10000);
			int enc = 0;
			for(int i = 0; i < 8; i++){
				enc |= (key & 0b1111) << (i * 4);
			}
			first ^= enc;
			second ^= enc;
			first = first << 8;
			first |= (key & 0b1111) << 4;
			first |= type;
			return new Tuple<Integer,Integer>(first,second);
		}
	}

	/**
	 * class Branch
	 *
	 * Represents a branch in the BUG pathfinding algorithm
	 *
	 * @branchPoint: The location the branch occurs at
	 * @branchedLeft: If this branch has already occured && went left
	 * @branchedRight: Like @branchedLeft, but to the right
	 * @target: Where we are trying to get
	 * @leftCanditate: The best choice for branching left, for lazy eval purposes
	 * @rightCanditate: Like left canditate, but to the right
	 * @bestCanditate: the choice between @leftCanditate and @rightCanditate that is closer to @target
	 * $branchesInCurrentPath: All branches that have been visited while going towards current @target
	 * $lastStatus: On the last move judged, whether we were on a left branch (-1), right branch (+1), or no branch (0)
	 * $last: all MapLocations that have been visited since last change in branch type, so we don't backtrack
	 *
	 */
	public static class Branch{
		public MapLocation branchPoint;
		public boolean branchedLeft = false,branchedRight = false;
		public MapLocation target;
		private MapLocation leftCanditate;
		private MapLocation rightCanditate;
		private MapLocation bestCanditate;
		public static ArrayList<Branch> branchesInCurrentPath = new ArrayList<Branch>();
		public static int lastStatus = 0;
		public static ArrayList<MapLocation> last = new ArrayList<MapLocation>();

		/**
		 * Constructor
		 *
		 * @param whereWeAre: current location -> @branchPoint
		 * @param targe: target location -> @target
		 *
		 */
		public Branch(MapLocation whereWeAre,MapLocation targe){
			branchPoint = whereWeAre;
			target = targe;
		}

		/**
		 * Branch fork
		 *
		 * handles creating new Branch for a point, and returning an old branch if we have visited it before
		 *
		 * @param here: current location
		 * @param targe: target location
		 * @n: new Branch for this spot
		 * @e: if there was an old branch that matches this one, this is it
		 *
		 * @return the new Branch if we haven't visited it before, the old one if we have
		 *
		 */
		public static Branch fork(MapLocation here,MapLocation targe){
			Branch n = new Branch(here,targe);
			Branch e = getFromEarlier(n);
			if(e == null){
				return n;
			}
			return e;
		}

		/**
		 * Branch getFromEarlier
		 *
		 * if there is an older Branch that describes this location, we want that instead
		 * 
		 * @param n: the potential new Branch
		 * 
		 * @return the first element in $branchesInCurrentPath that .equal(n), null if it doesn't exist
		 *
		 */
		public static Branch getFromEarlier(Branch n){
			for(int i = 0; i < branchesInCurrentPath.size(); i++){
				if(n.equal(branchesInCurrentPath.get(i))){
					return branchesInCurrentPath.get(i);
				}
			}
			return null;
		}

		/**
		 * resetPath
		 *
		 * resets all static members
		 *
		 */
		public static void resetPath(){
			branchesInCurrentPath = new ArrayList<Branch>();
			last = new ArrayList<MapLocation>();
			lastStatus = 0;
		}

		/**
		 * MapLocation bestBranch
		 *
		 * chooses the best place to branch out to
		 * 
		 * @return the closest MapLocation to @target in the best direction to go, based on stuff
		 *
		 */
		public MapLocation bestBranch(){
			if(branchedLeft && !branchedRight){ //If we have already branched left, and returned here, but haven't gone right, we'll try branching right
				lastStatus = 1;
				branchedRight = true; //document that we went right
				return getRightCanditate();
			}
			if(branchedRight && !branchedLeft){ //Similarly, if we've branched right and not left, try left
				lastStatus = -1;
				branchedLeft = true; // document that we went left
				return getLeftCanditate();
			}
			if(bestCanditate != null){ //If we've tried both, try what we originally thought was best
				lastStatus = bestCanditate.equals(leftCanditate) ? -1 : 1 ; //If it's left, set $lastStatus to -1, otherwise to +1
				return bestCanditate;
			}
			MapLocation left = getLeftCanditate(); //gets the best option on left branch
			MapLocation right = getRightCanditate(); //gets the best option on the right branch
			if(left == null && right != null){ //if no left options, but yes right options, then obvs go right
				lastStatus = 1;
				branchedRight = true; //document that we went right
				branchedLeft = true; //also document that we "went left" so we don't try in the future
				bestCanditate = right; //remember this as our best canditate
				return right;
			}else if(right == null && left != null){ //if no right options, but left, go left
				lastStatus = -1;
				branchedLeft = true; //document that we went left
				branchedRight = true; //document that we "went right" so we don't try in the future
				bestCanditate = left; //remember this as best canditate
				return left;
			}else if(right == null && left == null){
				return null; //if no options, we can't do shit
			}
			if(left.distanceSquaredTo(target) < right.distanceSquaredTo(target)){ //if left option is closer to @target, go left
				lastStatus = -1;
				branchedLeft = true; //document that we went left
				bestCanditate = left; //remember this as best canditate
				return left;
			}
			lastStatus = 1; //otherwise go right
			branchedRight = true; //document that we went right
			bestCanditate = right; //remember this as best canditate
			return right;
		}

		/**
		 * MapLocation getLeftCanditate
		 *
		 * gets the best MapLocation to move to in left branch
		 *
		 * Lazy evaluates: if we've done it before, doesn't do it again.
		 * 
		 * @return the MapLocation in the left branch whose distance to @target is smallest
		 *
		 */
		public MapLocation getLeftCanditate(){
			if(leftCanditate != null){ //if we've done this already, skip, because it can be costly
				return leftCanditate;
			}
			ArrayList<MapLocation> lefts = getAllLeftCanditates(); //gets all possibilities
			if(lefts.size() == 0){ //if no possibilities, return null
				return null;
			}
			int min = 0;
			int accmin = lefts.get(0).distanceSquaredTo(target); //searches for shortest distance element (O(n) time)
			for(int i = 1; i < lefts.size(); i++){
				int newMin = lefts.get(i).distanceSquaredTo(target);
				if(accmin > newMin){
					min = i;
					accmin = newMin;
				}
			}
			return lefts.get(min); //returns shortest distance element
		}

		/**
		 * MapLocation getRightCanditate
		 *
		 * gets the best MapLocation to move to in right branch
		 *
		 * Lazy evaluates: if we've done it before, doesn't do it again.
		 * 
		 * @return the MapLocation in the right branch whose distance to @target is smallest
		 *
		 */
		public MapLocation getRightCanditate(){
			if(rightCanditate != null){ //if we've done this already, skip, because it can be costly
				return rightCanditate;
			}
			ArrayList<MapLocation> rights = getAllRightCanditates(); //gets all possibilities
			if(rights.size() == 0){ //if no possibilities, return null
				return null;
			}
			int min = 0;
			int accmin = rights.get(0).distanceSquaredTo(target); //searches for shortest distance element (O(n) time)
			for(int i = 1; i < rights.size(); i++){
				int newMin = rights.get(i).distanceSquaredTo(target);
				if(accmin > newMin){
					min = i;
					accmin = newMin;
				}
			}
			return rights.get(min); //returns shortest distance element
		}

		/**
		 * ArrayList<MapLocation> getAllRightCanditates
		 *
		 * gets all possibilities in right branch (where wall is on left: named bc you turn right)
		 *
		 * @return all MapLocations adjacent to @branchPoint which are traversable (rubble<100) and have an intraversable tile (wall, rubble > 100) on the "left"
		 *
		 */
		public ArrayList<MapLocation> getAllRightCanditates(){
			ArrayList<MapLocation> base = new ArrayList<MapLocation>();
			MapLocation[] options = MapLocation.getAllMapLocationsWithinRadiusSq(branchPoint,3); //all squares adjacent to @branchPoint, including @branchPoint, bc it's less expensive than manually finding them
			try{
				//Most computation is here
				for(int i = 0; i < options.length; i++){ //Loop through all of @options
					boolean isRight = false; //assume it's not a right branch piece
					Direction forward = branchPoint.directionTo(options[i]); //what we consider "forward"
					int forwardInt = RESOURCE_FUNCTIONS.dirToInt(forward); //Only perform this once, for optimization
					//Next line is long. Gets an array of all directions considered "left" relative to @forward
					Direction[] rights = new Direction[]{RESOURCE_FUNCTIONS.intToDir((forwardInt + 5) % 8),RESOURCE_FUNCTIONS.intToDir((forwardInt + 6) % 8),RESOURCE_FUNCTIONS.intToDir((forwardInt + 7) % 8)};
					if(!branchPoint.equals(options[i]) && rc.senseRubble(options[i]) < 100 && rc.onTheMap(options[i])){ //checks this option to be: not current position, traversable, and on the map
						for(int j = 0; j < rights.length && !isRight; j++){ //as long as we're not sure it's right, loop through "left" directions and see if any are walls (rubble > 100)
							if(rc.senseRubble(options[i].add(rights[j])) > 100 && !last.contains(options[i])){ //right only if we haven't visited it recently, and there is rubble > 100 on "left"
								isRight = true;
							}
						}
					}
					if(isRight){
						base.add(options[i]); //if we decided it was right, add it
					}
				}
			}catch(Exception e){ //incase rc.senseRubble fails
				e.printStackTrace();
			}
			return base; //return that list!!
		}

		/**
		 * ArrayList<MapLocation> getAllLeftCanditates
		 *
		 * gets all possibilities in left branch (where wall is on right: named bc you turn left)
		 *
		 * @return all MapLocations adjacent to @branchPoint which are traversable (rubble<100) and have an intraversable tile (wall, rubble > 100) on the "right"
		 *
		 */
		public ArrayList<MapLocation> getAllLeftCanditates(){
			ArrayList<MapLocation> base = new ArrayList<MapLocation>();
			MapLocation[] options = MapLocation.getAllMapLocationsWithinRadiusSq(branchPoint,3); //all squares adjacent to @branchPoint, including @branchPoint, bc it's less expensive than manually finding them
			try{
				//Most computation is here
				for(int i = 0; i < options.length; i++){ //Loop through all of @options
					boolean isLeft = false; //assume it's not a left branch piece
					Direction forward = branchPoint.directionTo(options[i]); //what we consider "forward"
					int forwardInt = RESOURCE_FUNCTIONS.dirToInt(forward); //Only perform this once, for optimization
					//Next line is long. Gets an array of all directions considered "right" relative to @forward
					Direction[] lefts = new Direction[]{RESOURCE_FUNCTIONS.intToDir((forwardInt + 1) % 8),RESOURCE_FUNCTIONS.intToDir((forwardInt + 2) % 8),RESOURCE_FUNCTIONS.intToDir((forwardInt + 3) % 8)};
					if(rc.senseRubble(options[i]) < 100 && rc.onTheMap(options[i]) && !branchPoint.equals(options[i])){ //checks this option to be: not current position, traversable, and on the map
						for(int j = 0; j < lefts.length && !isLeft; j++){ //as long as we're not sure it's left, loop through "right" directions and see if any are walls (rubble > 100)
							if(rc.senseRubble(options[i].add(lefts[j])) > 100&& !last.contains(options[i])){ //left only if we haven't visited it recently, and there is rubble > 100 on "right"
								isLeft = true;
							}
						}
					}
					if(isLeft){
						base.add(options[i]); //if we decided it was left, add it
					}
				}
			}catch(Exception e){ //incase rc.senseRubble fails
				e.printStackTrace();
			}
			return base; //return that list!!
		}

		/**
		 * boolean equal
		 *
		 * checks if two objects (preferably two Branches) are equal
		 * NOT .equals!!! i would override, but it gave me an angry message about not overriding hashCode, so i went with .equal
		 * judges equality based on @branchPoint
		 *
		 * @param other: object to compare to
		 *
		 * @return if this Branch is at the same location as @other
		 *
		 */
		public boolean equal(Object other){
			Branch o = (Branch)other;
			if(o.branchPoint.equals(this.branchPoint) && o.target.equals(this.target)){
				return true;
			}
			return false;
		}
	}
}
