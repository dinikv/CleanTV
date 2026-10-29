package org.cleantv.launcher;
import java.util.*;
/** Explainable, local ranking. Eligibility always precedes preference and diversity. */
public final class PickEngine {
 public static final long DAY=86400000L;
 public static final class Item {
  public String id,title,language,mood,kind,provider; public int minutes;
  public boolean intense,beginner; public List<String> tags=new ArrayList<>();
 }
 public static final class Context {
  public String language="Any language",mood="Any mood",kind="Anything",mode="Balanced";
  public int limit; public long now=System.currentTimeMillis(),day=now/DAY;
  public Set<String> services=new HashSet<>(),preferred=new HashSet<>(),watched=new HashSet<>(),hidden=new HashSet<>(),saved=new HashSet<>();
  public Map<String,Long> skipped=new HashMap<>(),shown=new HashMap<>();
  public Map<String,Integer> tastes=new HashMap<>(),moods=new HashMap<>();
 }
 public static boolean eligible(Item i,Context c){
  if(!c.services.contains(i.provider)||c.watched.contains(i.id)||c.hidden.contains(i.id)||c.skipped.getOrDefault(i.id,0L)>c.now)return false;
  if(!PickRules.matches(i.language,i.minutes,i.mood,i.kind,c.language,c.limit,c.mood,c.kind))return false;
  if(c.mode.equals("French practice")&&!i.language.equals("French"))return false;
  if(c.mode.equals("Saved picks")&&!c.saved.contains(i.id))return false;
  return !c.mode.equals("Easy evening")||!i.intense;
 }
 public static int score(Item i,Context c){
  int value=c.preferred.contains(i.language)?18:0;
  value+=Math.min(5,Math.max(0,c.moods.getOrDefault(i.mood,0)))*5;
  int affinity=0;for(String tag:i.tags)affinity+=Math.max(-3,Math.min(3,c.tastes.getOrDefault(tag,0)))*7;
  value+=Math.max(-35,Math.min(35,affinity));
  if(c.saved.contains(i.id))value+=12;
  if(c.limit>0&&i.minutes>0)value+=Math.max(0,10-(c.limit-i.minutes)/10);
  if(c.mode.equals("Easy evening")){if(i.mood.equals("Light")||i.mood.equals("Heartfelt"))value+=18;if(i.minutes>0&&i.minutes<=100)value+=12;}
  if(c.mode.equals("Discovery")&&!c.preferred.contains(i.language))value+=26;
  if(c.mode.equals("French practice")&&i.beginner)value+=32;
  Long seen=c.shown.get(i.id);if(seen!=null){long age=Math.max(0,c.now-seen);if(age<DAY)value-=48;else if(age<3*DAY)value-=24;else if(age<7*DAY)value-=10;}
  // Small stable daily variation; never overrides an eligibility rule.
  value+=Math.floorMod((i.id+":"+c.day).hashCode(),11);
  return value;
 }
 public static List<Item> rank(List<Item> items,Context c){
  List<Item> remaining=new ArrayList<>(),result=new ArrayList<>();for(Item i:items)if(eligible(i,c))remaining.add(i);
  while(!remaining.isEmpty()){
   Item best=null;int bestValue=Integer.MIN_VALUE;
   for(Item i:remaining){int value=score(i,c);if(!result.isEmpty()){Item previous=result.get(result.size()-1);if(previous.language.equals(i.language))value-=10;if(previous.mood.equals(i.mood))value-=8;if(previous.provider.equals(i.provider))value-=3;}
    if(best==null||value>bestValue||(value==bestValue&&i.id.compareTo(best.id)<0)){best=i;bestValue=value;}}
   result.add(best);remaining.remove(best);
  }
  return result;
 }
 public static String reason(Item i,Context c){
  if(c.mode.equals("French practice"))return i.beginner?"French practice · beginner-focused lesson":"French practice · natural dialogue; choose subtitles in the app";
  String strongest=null;int weight=0;for(String tag:i.tags){int w=c.tastes.getOrDefault(tag,0);if(w>weight){strongest=tag;weight=w;}}
  if(strongest!=null)return "Because you liked "+strongest+" · "+i.mood.toLowerCase(Locale.ROOT);
  if(c.saved.contains(i.id))return "From your saved picks · ready when you are";
  if(c.mode.equals("Discovery")&&!c.preferred.contains(i.language))return "A "+i.language+" discovery beyond your preferred languages";
  if(c.limit>0&&i.minutes>0)return "About "+i.minutes+" min"+(i.kind.equals("Series")?" per episode":"")+" · within your selected time filter";
  if(c.mode.equals("Easy evening"))return "An easier evening · intense titles excluded";
  if(!c.shown.containsKey(i.id))return "No recent suggestion recorded · "+i.language+" · "+i.mood.toLowerCase(Locale.ROOT);
  return "Matches your filters · varied using your recent suggestions";
 }
}
