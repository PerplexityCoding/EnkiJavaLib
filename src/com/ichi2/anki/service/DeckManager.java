package com.ichi2.anki.service;

import java.io.File;
import java.io.FileFilter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ichi2.anki.BackupManager;
import com.ichi2.anki.model.Deck;

public class DeckManager {
	
	public static Logger log = LoggerFactory.getLogger(DeckManager.class);

	public static final int REQUESTING_ACTIVITY_STUDYOPTIONS = 0;
	public static final int REQUESTING_ACTIVITY_DECKPICKER = 1;
	public static final int REQUESTING_ACTIVITY_WIDGETSTATUS = 2;
	public static final int REQUESTING_ACTIVITY_BIGWIDGET = 3;
	public static final int REQUESTING_ACTIVITY_STATISTICS = 4;
	public static final int REQUESTING_ACTIVITY_SYNCCLIENT = 5;
	public static final int REQUESTING_ACTIVITY_CARDEDITOR = 6;
	public static final int REQUESTING_ACTIVITY_DOWNLOADMANAGER = 7;
	
	private static HashMap<String, DeckInformation> sLoadedDecks = new HashMap<String, DeckInformation>();
	private static HashMap<String, ReentrantLock> sDeckLocks = new HashMap<String, ReentrantLock>();
	
	private String deckPath = "E:\\Project\\Java Anki\\Decks";
	
	private static String mainDeckPath;
	
	public DeckManager() {
		
	}
	
	public synchronized static Deck getDeck(String deckpath, boolean setAsMainDeck, boolean doSafetyBackupIfNeeded, int requestingActivity, boolean rebuild) {
		Deck deck = null;
		lockDeck(deckpath);
		try {
			if (sLoadedDecks.containsKey(deckpath)) {
				// do not open deck if already loaded
				DeckInformation deckInformation = sLoadedDecks.get(deckpath);
	        	deck = deckInformation.mDeck;                	

			} else {
		        try {
		        	log.info("DeckManager: try to load deck " + deckpath + " (" + requestingActivity + ")");
		            /*if (doSafetyBackupIfNeeded) {
		            	BackupManager.safetyBackupNeeded(deckpath, BackupManager.SAFETY_BACKUP_THRESHOLD);
		            }
		            */
		            deck = Deck.openDeck(deckpath, rebuild, true);
		            log.info("DeckManager: Deck loaded!");
		            sLoadedDecks.put(deckpath, new DeckInformation(deckpath, deck, requestingActivity, rebuild));
				} catch (RuntimeException e) {
		            log.error("DeckManager: deck " + deckpath + " could not be opened = " + e.getMessage(), e);
					BackupManager.restoreDeckIfMissing(deckpath);
					deck = null;
		        } catch (SQLException e) {
		        	log.error("DeckManager: deck " + deckpath + " could not be opened = " + e.getMessage(), e);
		        	BackupManager.restoreDeckIfMissing(deckpath);
		        	deck = null;
				}
			}
		} finally {
			if (setAsMainDeck && deck != null) {
				mainDeckPath = deckpath;
			}
			unlockDeck(deckpath);
		}
		return deck;
	}
	
	
	
	
	/**
	 * Return anki deck lists
	 * 
	 * @return
	 */
	public Map<String, String> listAnkiDeck() {
		File decksPathFile = new File(deckPath);
		File[] fileList = decksPathFile.listFiles(new AnkiFilter());
		
		Map<String, String> sDeckPaths = new HashMap<String, String>();

		if (fileList != null && fileList.length > 0) {
			for (File file : fileList) {
				String name = file.getName().replaceAll(".anki", "");
				sDeckPaths.put(name, file.getAbsolutePath());
			}
		}
		
		return sDeckPaths;
	}
	
	public static void lockDeck(String path) {
		if (!sDeckLocks.containsKey(path)) {
			sDeckLocks.put(path, new ReentrantLock(true));
		}
		sDeckLocks.get(path).lock();
	}


	public static void unlockDeck(String path) {
		if (sDeckLocks.containsKey(path)) {
			sDeckLocks.get(path).unlock();
		}
	}
	
	public static final class AnkiFilter implements FileFilter {
		public boolean accept(File pathname) {
			if (pathname.isFile() && pathname.getName().endsWith(".anki")) {
				return true;
			}
			return false;
		}
	}
	
	
	public static class DeckInformation {
		public String mKey;
		public Deck mDeck;
		public boolean mInitiallyRebuilt = true;
		//public boolean mDeleteJournalModeForced = false;
		public boolean mWaitForDeckTaskToFinish = false;
		public ArrayList<Integer> mOpenedBy = new ArrayList<Integer>();

		DeckInformation(String key, Deck deck, int openedBy, boolean initiallyRebuilt) {
			this.mKey = key;
			this.mDeck = deck;
			this.mOpenedBy.add(openedBy);
			this.mInitiallyRebuilt = initiallyRebuilt;
		}
	}
	
	public static class CloseDeckInformation {
		public String mDeckPath;
		public int mCaller;

		CloseDeckInformation(String deckpath, int caller) {
			this.mDeckPath = deckpath;
			this.mCaller = caller;
		}
	}
	
}
