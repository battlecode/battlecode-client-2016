package battlecode.client.util;

import battlecode.server.Config;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public abstract class DataFile extends Observable {
  private String pathname;
  private long lastModified;

  private static Set<DataFile> instances =
    Collections.synchronizedSet(new HashSet<DataFile>());

  public DataFile(String pathname) {
    this.pathname = pathname;
    
    if(battlecode.server.Config.getGlobalConfig().getBoolean("bc.client.applet")) {
        URL url = null;
        try {
            url = new URL(battlecode.server.Config.getGlobalConfig().get("bc.client.applet.path") + pathname);
            load(url);
        } catch (MalformedURLException ex) {
            System.out.println("EXCEPTION URL" + url.getPath() );
            ex.printStackTrace();
        }
    } else {
        File f = new File(pathname);
        lastModified = f.lastModified();
        load(f);
    }
    
    instances.add(this);
    if (instances.size() == 1) {
      Thread thread = new Thread() {
	  public void run() {
	    while (!instances.isEmpty()) {
	      try {
		for (DataFile df: instances) {
		  df.refresh();
		  try { sleep(250 + 1000/instances.size()); }
		  catch (InterruptedException e) {}
		}
	      }
	      catch (ConcurrentModificationException e) {}
	    }
	  }
	};
      thread.setDaemon(true);
      thread.start();
    }
  }
  public void unload() { instances.remove(this); }
  
  protected abstract void load(File file);
  protected abstract void load(URL url);
  protected abstract void reload(File file);
  protected abstract void reload(URL url);
  
  private void refresh() {
    File file = new File(pathname);
    if (lastModified < file.lastModified()) {
      //System.out.println("reloading " + pathname);
      lastModified = file.lastModified();
      reload(file);
      setChanged();
      notifyObservers();
    }
  }
}
