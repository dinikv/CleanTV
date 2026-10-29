package org.cleantv.launcher;
/** Pure selection rules: never silently loosen a user's filters. */
public final class PickRules {
 public static boolean matches(String language,int minutes,String mood,String kind,String wantedLanguage,int limit,String wantedMood,String wantedKind){
  return (wantedLanguage.equals("Any language")||language.equals(wantedLanguage)) && (limit==0||(minutes>0&&minutes<=limit)) && (wantedMood.equals("Any mood")||mood.equals(wantedMood)) && (wantedKind.equals("Anything")||kind.equals(wantedKind));
 }
 public static int score(String language,int minutes,int affinity){return (language.equals("English")||language.equals("Tamil")||language.equals("French")?20:0)+(minutes>0&&minutes<=120?5:0)+affinity*10;}
}
