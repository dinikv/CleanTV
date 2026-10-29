import org.cleantv.launcher.PickRules;
public class PickRulesTest {
 static void check(boolean value,String reason){if(!value)throw new AssertionError(reason);}
 public static void main(String[] args){
 check(PickRules.matches("French",81,"Heartfelt","Film","French",90,"Any mood","Anything"),"French film fits 90 minute evening");
 check(!PickRules.matches("Tamil",177,"Heartfelt","Film","Any language",120,"Any mood","Anything"),"Long film excluded by time budget");
 check(!PickRules.matches("French",0,"Light","Video","French",30,"Any mood","Anything"),"Unknown runtime never promises to fit budget");
 check(PickRules.matches("French",0,"Light","Video","French",0,"Light","Video"),"Unknown duration available with no duration filter");
 check(!PickRules.matches("Korean",109,"Thrilling","Film","French",0,"Any mood","Anything"),"Language constraint preserved");
 check(!PickRules.matches("English",110,"Light","Film","Any language",0,"Heartfelt","Anything"),"Mood constraint preserved");
 check(!PickRules.matches("English",110,"Light","Film","Any language",0,"Any mood","Video"),"Format constraint preserved");
 check(PickRules.score("French",81,0)>PickRules.score("Korean",109,0),"Preferred languages prioritised");
 check(PickRules.score("English",140,2)>PickRules.score("English",110,0),"Explicit mood feedback changes ranking");
 System.out.println("9 recommendation rule checks passed");}
}
