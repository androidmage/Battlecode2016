package zombomb; //So the program knows what to call our bot

import battlecode.common.*; //imports Battlecode UI
import java.util.Random;	//Use this instead of Math.random(); seeded by the robot's id so more likely to be random than Math.random

public class RobotPlayer{
	/**
	 * Useful members
	 *
	 * $rc: the RobotController for this robot. Static so all methods can use it
	 * $randall: our source of all randomness
	 */
	static RobotController rc;
	static Random randall;
	static Team ourTeam;
	static Team opponentTeam;

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
		if(rc.getTeam() ==Team.A) {
			ourTeam = Team.A;
			opponentTeam = Team.B;
		}
		if(rc.getTeam() == Team.B) {
			ourTeam = Team.B;
			opponentTeam = Team.A;
		}
		if(selftype == RobotType.ARCHON){
			Archon s = new RobotPlayer().new Archon();
			s.run();
		}else if(selftype == RobotType.SCOUT){
			Scout s = new RobotPlayer().new Scout();
			s.run();
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
						if(RESOURCE_FUNCTIONS.tryBuild(RobotType.SCOUT)){ //See function in RESOURCE_FUNCTIONS to know what it does
							//After building scout, waits a turn, then signals it the location, so it has a good idea of where base is
							Clock.yield();
							rc.broadcastMessageSignal(0,0,9);
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
					//Do nothing until base-line information is gathered: ie, where are the archons. in future, archons may also give message assigning role
					if(base == null){
						Signal[] signals = rc.emptySignalQueue();
						if(signals.length > 0){ //if == 0, no signals, so not ready
							Tuple<Integer,Integer> approxxCoordinates = new Tuple<Integer,Integer>(0,0);
							//averages the signal's origins
							int counted = 0;
							for(int i = 0; i < signals.length; i++){
								if(signals[i].getTeam() == rc.getTeam() && rc.senseRobot(signals[i].getID()).type == RobotType.ARCHON){
									approxxCoordinates.first += signals[i].getLocation().x;
									approxxCoordinates.second += signals[i].getLocation().y;
									counted++;
								}
							}
							if(counted > 0){
								approxxCoordinates.first /= counted;
								approxxCoordinates.second /= counted;

								//sets @base to this
								base = new MapLocation(approxxCoordinates.first,approxxCoordinates.second);
								rc.setIndicatorString(0,"x:" + base.x + "::y:" + base.y);
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
			if(zombos.length >= 3 || zombos.length >= disciples){
				disciples = zombos.length;
				return true;
			}
			return false;
		}
		/*
		 * Scan for all enemy robots
		 * Finds enemy Archon
		 */
		public boolean scanArchon() {
			RobotInfo[] robots;
			robots = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, opponentTeam);
			for(int i = 0; i < robots.length; i++) {
				if(robots[i].type == RobotType.ARCHON) {
					return true;
				}
			}
			return false;
		}
	}
	
	public MapLocation scanArchonLocation() {
		RobotInfo[] robots;
		robots = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, opponentTeam);
		int pos = 0;
		for(int i = 0; i < robots.length; i++) {
			if(robots[i].type == RobotType.ARCHON) {
				pos = i;
			}
		}
		return robots[pos].location;
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
			Direction d = Direction.NORTH;
			for(int j = 0; j < i; j++){
				d = d.rotateRight();
			}
			return d;
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
			if(d.equals(Direction.NONE)){
				return -1;
			}
			int i = 0;
			while(!d.equals(Direction.NORTH)){
				d = d.rotateLeft();
				i++;
			}
			return i;
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
			Tuple<MapLocation, Double> locationAndSize = new RobotPlayer().new Tuple<MapLocation, Double>(maxPileLocation, maxPileSize);
			
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
			rc.setIndicatorString(1,"max:none");
			return false;
		}
	}
	
	/**
	 * Tuple
	 * 
	 * a simple tuple class so that tuples can be used.
	 */
	public class Tuple<X, Y> { 
		  public X first; 
		  public Y second; 
		  public Tuple(X first, Y second) { 
		    this.first = first; 
		    this.second = second; 
		  } 
	} 
}

