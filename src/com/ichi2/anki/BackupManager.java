/***************************************************************************************
 * Copyright (c) 2011 Norbert Nagold <norbert.nagold@gmail.com>                         *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.anki;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.UnknownFormatConversionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ichi2.anki.db.AnkiDatabaseManager;

public class BackupManager {

	private static Logger log = LoggerFactory.getLogger(BackupManager.class);
	
	static String mBackupDirectoryPath;
	static String mBrokenDirectoryPath;
	static int mMaxBackups;
	static File mLastCreatedBackup;
	static File[] mLastDeckBackups;

	public final static int RETURN_BACKUP_CREATED = 0;
	public final static int RETURN_ERROR = 1;
	public final static int RETURN_TODAY_ALREADY_BACKUP_DONE = 2;
	public final static int RETURN_NOT_ENOUGH_SPACE = 3;
	public final static int RETURN_DECK_NOT_CHANGED = 4;
	public final static int RETURN_DECK_RESTORED = 5;
	public final static int RETURN_NULL = 6;
	public final static int RETURN_LOW_SYSTEM_SPACE = 7;
	public final static int RETURN_BACKUP_NEEDED = 8;

	public final static int MIN_FREE_SPACE = 10;
	
	public final static String BACKUP_SUFFIX = "/backup";
	public final static String BROKEN_DECKS_SUFFIX = "/broken";

	
	
	private static ArrayList<String> mDeckPickerDecks;
	private static boolean mUseBackups = true;

 	/** Number of day, after which a backup is done on first non-studyoptions-opening (for safety reasons) */
	public static final int SAFETY_BACKUP_THRESHOLD = 3;

	
    /* Prevent class from being instantiated */
	private BackupManager() {
	}


	public static void initBackup() {
		// FIXME #URGENT !!
		mUseBackups = true;
		// FIXME #URGENT !!
		
		mDeckPickerDecks = new ArrayList<String>();
	}


	public static boolean isActivated() {
		return mUseBackups;
	}


	private static File getBackupDirectory() {
		// FIXME #URGENT !!
		int maxBackups = 3;
		String deckPath = "";
		// FIXME #URGENT !!
		
        if (mBackupDirectoryPath == null) {
        	mBackupDirectoryPath = deckPath + BACKUP_SUFFIX;
        	mMaxBackups = maxBackups;
        }
        File directory = new File(mBackupDirectoryPath);
        if (!directory.isDirectory()) {
        	directory.mkdirs();
        }
        return directory;
	}


	private static File getBrokenDirectory() {
		// FIXME #URGENT !!
		String deckPath = "";
		// FIXME #URGENT !!
		
        if (mBrokenDirectoryPath == null) {
        	mBrokenDirectoryPath = deckPath + BROKEN_DECKS_SUFFIX;
        }
        File directory = new File(mBrokenDirectoryPath);
        if (!directory.isDirectory()) {
        	directory.mkdirs();
        }
        return directory;
	}


	/** If deck has not been opened for a long time, we perform a backup here because Android deleted sometimes corrupted decks */
	public static boolean safetyBackupNeeded(String deckpath, int days) {
		if (mDeckPickerDecks == null) {
			initBackup();
		}
		if (!mUseBackups || mDeckPickerDecks.contains(deckpath)) {
			return false;
		}
	        File[] deckBackups = getDeckBackups(new File(deckpath));
	        int len = deckBackups.length;
		if (len == 0) {
			// no backup available
			return true;
		}
		String backupDateString = deckBackups[len - 1].getName().replaceAll("^.*-(\\d{4}-\\d{2}-\\d{2}).anki$", "$1");
		Date backupDate;
		try {
			backupDate = new SimpleDateFormat("yyyy-MM-dd").parse(backupDateString);
		} catch (ParseException e) {
			log.error("BackupManager - safetyBackupNeeded - Error on parsing backups: " + e);
			return true;
		}
        Date target = Utils.genToday(Utils.utcOffset() + (days * 86400));
		if (backupDate.before(target)) {
			return true;
		} else {
			mDeckPickerDecks.add(deckpath);
			return false;
		}
	}


	/** Restores the current deck from backup if Android deleted it */
	public static void restoreDeckIfMissing(String deckpath) {
		if (mUseBackups && !(new File(deckpath)).exists()) {
			log.error("BackupManager: Deck " + deckpath + " has been deleted by Android. Restoring it:");
			File[] fl = BackupManager.getDeckBackups(new File(deckpath));
			if (fl.length > 0) {
				log.error("BackupManager: Deck " + deckpath + " successfully restored");
				BackupManager.restoreDeckBackup(deckpath, fl[fl.length - 1].getAbsolutePath());					
			} else {
				log.error("BackupManager: Deck " + deckpath + " could not be restored");
			}
		}
	}


	public static int backupDeck(String deckpath) {
		if (mDeckPickerDecks == null) {
			initBackup();
		}
		mDeckPickerDecks.add(deckpath);
		mLastCreatedBackup = null;
		mLastDeckBackups = null;
        File deckFile = new File(deckpath);
        File[] deckBackups = getDeckBackups(deckFile);
        int len = deckBackups.length;
        if (len > 0 && deckBackups[len - 1].lastModified() == deckFile.lastModified()) {
    		deleteDeckBackups(deckBackups, mMaxBackups);
        	return RETURN_DECK_NOT_CHANGED;
        }
        Date value = Utils.genToday(Utils.utcOffset());

        String backupFilename;
        try {
        	backupFilename = String.format(Utils.ENGLISH_LOCALE, deckFile.getName().replace(".anki", "") + "-%tF.anki", value);        	
        } catch (UnknownFormatConversionException e) {
        	log.error("backupDeck: error on creating backup filename: ", e);
        	return RETURN_ERROR;
        }

        File backupFile = new File(getBackupDirectory().getPath(), backupFilename);
        if (backupFile.exists()) {
            log.info("No new backup of " + deckFile.getName() + " created. Already made one today");
    		deleteDeckBackups(deckBackups, mMaxBackups);
            return RETURN_TODAY_ALREADY_BACKUP_DONE;
        }

        if (getFreeDiscSpace(deckFile) < deckFile.length() + (MIN_FREE_SPACE * 1024 * 1024)) {
            log.error("Not enough space on sd card to backup " + deckFile.getName() + ".");
        	return RETURN_NOT_ENOUGH_SPACE;
        }

        try {
            InputStream stream = new FileInputStream(deckFile);
            Utils.writeToFile(stream, backupFile.getAbsolutePath());
            stream.close();

            // set timestamp of file in order to avoid creating a new backup unless its changed
            backupFile.setLastModified(deckFile.lastModified());
        } catch (IOException e) {
            log.error("Backup file " + deckFile.getName() + " - Copying of file failed.", e);
            return RETURN_ERROR;
        }

        mLastCreatedBackup = backupFile;
        mLastDeckBackups = deckBackups;
        return RETURN_BACKUP_CREATED;
	}


	public static long getFreeDiscSpace(String path) {
		return getFreeDiscSpace(new File(path));
	}
	public static long getFreeDiscSpace(File file) {
		try {
			
			/*
			StatFs stat = new StatFs(file.getParentFile().getPath());
			
	    	long blocks = stat.getAvailableBlocks();
	    	long blocksize = stat.getBlockSize();
	    	return blocks * blocksize; */
	    	
	    	//FIXME: PAS SUR
	    	return file.getFreeSpace();
	    	
		} catch (IllegalArgumentException e) {
			log.error("Free space could not be retrieved: " + e);
			return MIN_FREE_SPACE * 1024 * 1024;
		}	
	}


	public static boolean cleanUpAfterBackupCreation(boolean deckLoaded) {
		if (deckLoaded) {
			return deleteDeckBackups(mLastDeckBackups, mMaxBackups - 1);
		} else if (mLastCreatedBackup != null) {
			return mLastCreatedBackup.delete();
		}
		return false;
	}


	public static int restoreDeckBackup(String deckpath, String backupPath) {
        // rename old file and move it to subdirectory
    	if ((new File(deckpath)).exists() && !moveDeckToBrokenFolder(deckpath)) {
    		return RETURN_ERROR;
    	}

    	// copy backup to new position and rename it
    	File backupFile = new File(backupPath);
    	File deckFile = new File(deckpath);
        if (getFreeDiscSpace(deckFile) < deckFile.length() + (MIN_FREE_SPACE * 1024 * 1024)) {
            log.error("Not enough space on sd card to restore " + deckFile.getName() + ".");
        	return RETURN_NOT_ENOUGH_SPACE;
        }
        try {
            InputStream stream = new FileInputStream(backupFile);
            Utils.writeToFile(stream, deckFile.getAbsolutePath());
            stream.close();

            // set timestamp of file in order to avoid creating a new backup unless its changed
            deckFile.setLastModified(backupFile.lastModified());
        } catch (IOException e) {
            log.error("Restore of file " + deckFile.getName() + " failed.", e);
            return RETURN_ERROR;
        }
		return RETURN_DECK_RESTORED;
	}


	public static boolean repairDeck(String deckPath) {
		File deckFile = new File(deckPath);
		AnkiDatabaseManager.closeDatabase(deckPath);

    	// repair file
    	String execString = "sqlite3 " + deckPath + " .dump | sqlite3 " + deckPath + ".tmp";
    	log.info("repairDeck - Execute: " + execString);
    	try {
    		String[] cmd = {"/system/bin/sh", "-c", execString };
    	    Process process = Runtime.getRuntime().exec(cmd);
    	    process.waitFor();

    		// move deck to broken folder
    		String brokenDirectory = getBrokenDirectory().getPath();
    		Date value = Utils.genToday(Utils.utcOffset());
            String movedFilename = String.format(Utils.ENGLISH_LOCALE, deckFile.getName().replace(".anki", "") + "-corrupt-%tF.anki", value);
            File movedFile = new File(brokenDirectory, movedFilename);
            int i = 1;
            while (movedFile.exists()) {
            	movedFile = new File(brokenDirectory, movedFilename.replace(".anki", "-" + Integer.toString(i) + ".anki"));
            	i++;
            }
            movedFilename = movedFile.getName();
        	if (!deckFile.renameTo(movedFile)) {
        		return false;
        	}
        	log.info("repairDeck - moved corrupt file to " + movedFile.getAbsolutePath());
        	File repairedFile = new File(deckPath + ".tmp");
        	if (!repairedFile.renameTo(deckFile)) {
        		return false;
        	}
        	return true;
    	} catch (IOException e) {
    		log.error("repairDeck - error: ", e);
    	} catch (InterruptedException e) {
    		log.error("repairDeck - error: ", e);
    	}
    	return false;
	}


	public static boolean moveDeckToBrokenFolder(String deckPath) {
		File deckFile = new File(deckPath);
		AnkiDatabaseManager.closeDatabase(deckPath);
        Date value = Utils.genToday(Utils.utcOffset());
        String movedFilename = String.format(Utils.ENGLISH_LOCALE, deckFile.getName().replace(".anki", "") + "-corrupt-%tF.anki", value);
        File movedFile = new File(getBrokenDirectory().getPath(), movedFilename);
        int i = 1;
        while (movedFile.exists()) {
        	movedFile = new File(getBrokenDirectory().getPath(), movedFilename.replace(".anki", "-" + Integer.toString(i) + ".anki"));
        	i++;
        }
        movedFilename = movedFile.getName();
    	if (!deckFile.renameTo(movedFile)) {
    		return false;
    	}
    	
    	// move all connected files (like journals, directories...) too
    	String deckName = deckFile.getName();
    	File directory = new File(deckFile.getParent());
    	for (File f : directory.listFiles()) {
    		if (f.getName().startsWith(deckName)) {
    			if (!f.renameTo(new File(getBrokenDirectory().getPath(), f.getName().replace(deckName, movedFilename)))) {
    				return false;
    			}
    		}
    	}
    	return true;
	}


	public static File[] getDeckBackups(File deckFile) {
		File[] files = getBackupDirectory().listFiles();
		ArrayList<File> deckBackups = new ArrayList<File>();
		for (File aktFile : files){
			if (aktFile.getName().replaceAll("^(.*)-\\d{4}-\\d{2}-\\d{2}.anki$", "$1.anki").equals(deckFile.getName())) {
				deckBackups.add(aktFile);
			}
		}
		Collections.sort(deckBackups);
		File[] fileList = new File[deckBackups.size()];
		deckBackups.toArray(fileList);
		return fileList;
	}


	public static boolean removeDeck(File deckFile) {
    	String deckName = deckFile.getName();
    	File directory = new File(deckFile.getParent());
    	for (File f : directory.listFiles()) {
    		if (f.getName().startsWith(deckName)) {
    			if (!removeDir(f)) {
    				return false;
    			}
    		}
    	}
    	return true;
	}


	public static boolean deleteDeckBackups(String deckpath, int keepNumber) {
		return deleteDeckBackups(getDeckBackups(new File(deckpath)), keepNumber);
	}
	public static boolean deleteDeckBackups(File deckFile, int keepNumber) {
		return deleteDeckBackups(getDeckBackups(deckFile), keepNumber);
	}
	public static boolean deleteDeckBackups(File[] deckBackups, int keepNumber) {
    	if (deckBackups == null) {
    		return false;
    	}
		for (int i = 0; i < deckBackups.length - keepNumber; i++) {
    		deckBackups[i].delete();
    	}
		return true;
	}


	public static boolean deleteAllBackups() {
		return removeDir(getBackupDirectory());
	}


	public static boolean removeDir(File dir){
		if (dir.isDirectory()){
			File[] files = dir.listFiles();
			for (File aktFile: files){
				removeDir(aktFile);
			}
		}
		return dir.delete();
	}
}
