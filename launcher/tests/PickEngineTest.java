import org.cleantv.launcher.*;
import java.util.*;
public class PickEngineTest {
 static int checks=0;
 static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
 static PickEngine.Item item(String id,String language,String mood,int minutes){PickEngine.Item i=new PickEngine.Item();i.id=id;i.title=id;i.language=language;i.mood=mood;i.minutes=minutes;i.kind="Film";i.provider="Netflix";i.tags=Arrays.asList("mysteries","friendship");return i;}
 static PickEngine.Context context(){PickEngine.Context c=new PickEngine.Context();c.now=20*PickEngine.DAY;c.day=20;c.services.add("Netflix");c.preferred.addAll(Arrays.asList("English","Tamil","French"));return c;}
 public static void main(String[] args){
 PickEngine.Item a=item("a","English","Light",90),b=item("b","Korean","Thrilling",100);PickEngine.Context c=context();
 check(PickEngine.eligible(a,c),"eligible base");
 c.services.clear();check(!PickEngine.eligible(a,c),"disabled/uninstalled service excluded");c=context();
 c.watched.add("a");check(!PickEngine.eligible(a,c),"watched excluded");c=context();
 c.hidden.add("a");check(!PickEngine.eligible(a,c),"disliked title excluded");c=context();
 c.skipped.put("a",c.now+1);check(!PickEngine.eligible(a,c),"temporary skip active");c.skipped.put("a",c.now-1);check(PickEngine.eligible(a,c),"skip expires");
 c.limit=89;check(!PickEngine.eligible(a,c),"duration hard filter");c.limit=90;check(PickEngine.eligible(a,c),"duration boundary");a.minutes=0;check(!PickEngine.eligible(a,c),"unknown duration excluded with time filter");a.minutes=90;c=context();
 c.language="French";check(!PickEngine.eligible(a,c),"explicit language respected");c=context();c.kind="Series";check(!PickEngine.eligible(a,c),"format respected");c=context();c.mood="Heartfelt";check(!PickEngine.eligible(a,c),"mood respected");c=context();
 c.mode="Saved picks";check(!PickEngine.eligible(a,c),"unsaved excluded");c.saved.add("a");check(PickEngine.eligible(a,c),"saved allowed");c.watched.add("a");check(!PickEngine.eligible(a,c),"saved does not override watched");c=context();
 c.mode="French practice";check(!PickEngine.eligible(a,c),"French practice hard language");PickEngine.Item f=item("f","French","Light",0);f.kind="Video";f.beginner=true;check(PickEngine.eligible(f,c),"French lesson with no length filter");check(PickEngine.reason(f,c).contains("beginner"),"beginner reason evidence");c=context();
 c.mode="Easy evening";a.intense=true;check(!PickEngine.eligible(a,c),"easy evening excludes intense");a.intense=false;c=context();
 int base=PickEngine.score(a,c);c.tastes.put("mysteries",2);check(PickEngine.score(a,c)>base,"likes boost similar themes");check(PickEngine.reason(a,c).contains("mysteries"),"explain actual learned feature");c.tastes.put("mysteries",-2);check(PickEngine.score(a,c)<base,"dislikes reduce similar themes");c=context();
 c.shown.put("a",c.now);check(PickEngine.score(a,c)<base,"recent impressions penalized");c.shown.put("a",c.now-8*PickEngine.DAY);check(PickEngine.score(a,c)==base,"history penalty expires");c=context();
 int normal=PickEngine.score(b,c);c.mode="Discovery";check(PickEngine.score(b,c)>normal,"discovery boosts outside preferred languages");c.language="French";check(PickEngine.rank(Arrays.asList(a,b),c).isEmpty(),"discovery never relaxes explicit language");c=context();
 List<PickEngine.Item> ranked=PickEngine.rank(Arrays.asList(a,b),c);check(ranked.size()==2&&new HashSet<>(ranked).size()==2,"no duplicate suggestions");check(ranked.equals(PickEngine.rank(Arrays.asList(a,b),c)),"stable ranking within same day and context");
 c.saved.add("a");check(PickEngine.score(a,c)>base,"saved title has priority");
 System.out.println(checks+" smart recommendation checks passed");
 }
}
