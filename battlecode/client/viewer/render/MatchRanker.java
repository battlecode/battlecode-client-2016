package battlecode.client.viewer.render;

import java.io.*;
import java.util.*;

import battlecode.client.*;
import battlecode.client.viewer.*;
import battlecode.common.Team;

public class MatchRanker {
   
    public static boolean EOF = false; 
    
	public static void main(String args[]) {
        
//        Set<String> teams = new HashSet<String>(Arrays.asList(new String[] {
//            "team032", "team078", "team043", "team063",
//            "team042", "team106", "team002", "team124"
//        }));
        
        Map<String, List<Integer>> teamArchons = new HashMap<String, List<Integer>>();
        Map<String, List<Double>> teamProduction = new HashMap<String, List<Double>>();
        Set<String> done = new HashSet<String>();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        
        String file;
        try {
            while ((file = in.readLine()) != null) {
                
                file = file.trim();
                
                if (file == "")
                    continue;
                
                if (done.contains(file))
                    continue;
                else done.add(file);
                
                String[] fields = file.split("-");
                
                System.out.println(":: " + file);
                
                if (fields.length < 5)
                    continue; 
                
                String teamA = fields[3];
                String teamB = fields[4];
                
                file = "/media/robocraft/final-matches/" + file;
                
                ClientProxy proxy;
            	try {
            		proxy = new StreamClientProxy(file);
            	}
            	catch (java.io.IOException e) {
            		e.printStackTrace();
            		return;
            	}
                outer: while(true) {
                    try {
                    BufferedMatch match = new BufferedMatch(proxy);
                    GameStateTimeline<DrawState> timeline =
                        new GameStateTimeline<DrawState>(match, DrawState.FACTORY, 10);
                    DrawState ds = new DrawState();
                    timeline.setTargetState(ds);
                    while (true) {
                        if (match.isFinished() &&
                                timeline.getNumRounds() == match.getRoundsAvailable()) {
                            break;
                        }
                        try {Thread.sleep(10); } catch (Exception e) {}
                        if (EOF) {
                            EOF = false;
                            break outer;
                        }
                    }
                    timeline.setRound(timeline.getNumRounds());

                    double totalProduction = 0;

                    List<DrawObject> archons = ds.getArchons(Team.A);
                    
                    if (!teamArchons.containsKey(teamA))
                        teamArchons.put(teamA, new LinkedList<Integer>());
                    teamArchons.get(teamA).add(archons.size());
                        
                    for (DrawObject obj: archons) {
                        totalProduction += obj.getProduction();
                    }

                    archons = ds.getArchons(Team.B);
                    
                    if (!teamArchons.containsKey(teamB))
                        teamArchons.put(teamB, new LinkedList<Integer>());
                    teamArchons.get(teamB).add(archons.size());
                    
                    for (DrawObject obj: archons) {
                        totalProduction -= obj.getProduction();
                    }
                    
                    if (!teamProduction.containsKey(teamA))
                        teamProduction.put(teamA, new LinkedList<Double>());
                    teamProduction.get(teamA).add(totalProduction);
                    
                    if (!teamProduction.containsKey(teamB))
                        teamProduction.put(teamB, new LinkedList<Double>());
                    teamProduction.get(teamB).add(-totalProduction);
                    
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        for (String team : teamArchons.keySet())
            //if (teams.contains(team))
                System.out.println(team + "\t" + average(teamArchons.get(team)));
        
        for (String team : teamProduction.keySet())
            //if (teams.contains(team))
                System.out.println(team + "\t" + average(teamProduction.get(team)));
        
        //System.out.println(teamArchons);
        //System.out.println(teamProduction);
	}

    private static Number average(List<? extends Number> list) {
        Double total = 0.0;
        for (Number n : list)
            total = total + n.doubleValue();
        return total / list.size();
    }
    
}
