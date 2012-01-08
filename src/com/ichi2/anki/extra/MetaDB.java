package com.ichi2.anki.extra;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ichi2.anki.DeckStatus;
import com.ichi2.anki.db.AnkiDb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLException;


/**
 * Used to store additional information besides what is stored in the deck itself.
 * <p>
 * Currently it used to store:
 * <ul>
 *   <li>The languages associated with questions and answers.</li>
 *   <li>The state of the whiteboard.</li>
 *   <li>The cached state of the widget.</li>
 * </ul>
 */
public class MetaDB {
	
	public static Logger log = LoggerFactory.getLogger(MetaDB.class);
	
    /** The name of the file storing the meta-db. */
    private static final String DATABASE_NAME = "ankidroid.db";

    // Possible values for the qa column of the languages table.
    /** The language refers to the question. */
    public static final int LANGUAGES_QA_QUESTION = 0;
    /** The language refers to the answer. */
    public static final int LANGUAGES_QA_ANSWER = 1;
    /** The language does not refer to either the question or answer. */
    public static final int LANGUAGES_QA_UNDEFINED = 2;

    /** The pattern used to remove quotes from file names. */
    private static final Pattern quotePattern = Pattern.compile("[\"']");

    /** The database object used by the meta-db. */
    private static AnkiDb mMetaDb = null;


    /** Remove any pairs of quotes from the given text. */
    private static String stripQuotes(String text) {
        Matcher matcher = quotePattern.matcher(text);
        text = matcher.replaceAll("");
        return text;
    }


    /** Open the meta-db and creates any table that is missing. */
    private static void openDB(Context context) {
        try {
            mMetaDb = context.openOrCreateDatabase(DATABASE_NAME,  0, null);
            mMetaDb.execSQL(
                    "CREATE TABLE IF NOT EXISTS languages ("
                            + " _id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "deckpath TEXT NOT NULL, modelid INTEGER NOT NULL, "
                            + "cardmodelid INTEGER NOT NULL, "
                            + "qa INTEGER, "
                            + "language TEXT)");
            mMetaDb.execSQL(
                    "CREATE TABLE IF NOT EXISTS whiteboardState ("
                            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "deckpath TEXT NOT NULL, "
                            + "state INTEGER)");
            mMetaDb.execSQL(
                    "CREATE TABLE IF NOT EXISTS customDictionary ("
                            + "_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                            + "deckpath TEXT NOT NULL, "
                            + "dictionary INTEGER)");
            mMetaDb.execSQL(
                    "CREATE TABLE IF NOT EXISTS widgetStatus ("
                    + "deckPath TEXT NOT NULL PRIMARY KEY, "
                    + "deckName TEXT NOT NULL, "
                    + "newCards INTEGER NOT NULL, "
                    + "dueCards INTEGER NOT NULL, "
                    + "failedCards INTEGER NOT NULL, "
            		+ "eta INTEGER NOT NULL, "
            		+ "time INTEGER NOT NULL)");
            mMetaDb.execSQL(
                    "CREATE TABLE IF NOT EXISTS intentInformation ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "source TEXT NOT NULL, "
                    + "target INTEGER NOT NULL)");
            log.info("Opening MetaDB");
        } catch(Exception e) {
            log.error("Error", "Error opening MetaDB ", e);
        }
    }


    /** Open the meta-db but only if it currently closed. */
    private static void openDBIfClosed(Context context) {
        if (mMetaDb == null || !mMetaDb.isOpen()) {
            openDB(context);
        }
    }


    /** Close the meta-db. */
    public static void closeDB() {
        if (mMetaDb != null && mMetaDb.isOpen()) {
            mMetaDb.close();
            mMetaDb = null;
            log.info("Closing MetaDB");
        }
    }


    /** Reset the content of the meta-db, erasing all its content. */
    public static boolean resetDB(Context context) {
        openDBIfClosed(context);
        try {
            mMetaDb.execSQL("DROP TABLE IF EXISTS languages;");
            log.info("Resetting all language assignment");
            mMetaDb.execSQL("DROP TABLE IF EXISTS whiteboardState;");
            log.info("Resetting whiteboard state");
            mMetaDb.execSQL("DROP TABLE IF EXISTS customDictionary;");
            log.info("Resetting custom Dictionary");
            mMetaDb.execSQL("DROP TABLE IF EXISTS widgetStatus;");
            log.info("Resetting widget status");
            mMetaDb.execSQL("DROP TABLE IF EXISTS intentInformation;");
            log.info("Resetting intentInformation");
            return true;
        } catch(Exception e) {
            log.error("Error", "Error resetting MetaDB ", e);
        }
        return false;
    }


    /** Reset the language associations for all the decks and card models. */
    public static boolean resetLanguages(Context context) {
        if (mMetaDb == null || !mMetaDb.isOpen()) {
            openDB(context);
        }
        try {
            log.info("Resetting all language assignments");
            mMetaDb.execSQL("DROP TABLE IF EXISTS languages;");
            openDB(context);
            return true;
        } catch(Exception e) {
            log.error("Error", "Error resetting MetaDB ", e);
        }
        return false;
    }


    /** Reset the widget status. */
    public static boolean resetWidget(Context context) {
        if (mMetaDb == null || !mMetaDb.isOpen()) {
            openDB(context);
        }
        try {
            log.info("Resetting widget status");
            mMetaDb.execSQL("DROP TABLE IF EXISTS widgetStatus;");
            openDB(context);
            return true;
        } catch(Exception e) {
            log.error("Error", "Error resetting widgetStatus ", e);
        }
        return false;
    }


    /** Reset the intent information. */
    public static boolean resetIntentInformation(Context context) {
        if (mMetaDb == null || !mMetaDb.isOpen()) {
            openDB(context);
        }
        try {
            log.info("Resetting intent information");
            mMetaDb.execSQL("DROP TABLE IF EXISTS intentInformation;");
            openDB(context);
            return true;
        } catch(Exception e) {
            log.error("Error", "Error resetting intentInformation ", e);
        }
        return false;
    }

    /**
     * Associates a language to a deck, model, and card model for a given type.
     *
     * @param deckPath the deck for which to store the language association
     * @param modelId the model for which to store the language association
     * @param cardModelId the card model for which to store the language association
     * @param qa the part of the card for which to store the association, {@link #LANGUAGES_QA_QUESTION},
     *           {@link #LANGUAGES_QA_ANSWER}, or {@link #LANGUAGES_QA_UNDEFINED}
     * @param language the language to associate, as a two-characters, lowercase string
     */
    public static void storeLanguage(Context context, String deckPath, long modelId, long cardModelId, int qa,
            String language) {
        openDBIfClosed(context);
        deckPath = stripQuotes(deckPath);
        try {
            mMetaDb.execSQL(
                    "INSERT INTO languages (deckpath, modelid, cardmodelid, qa, language) "
                            + " VALUES (?, ?, ?, ?, ?);",
                            new Object[]{deckPath, modelId, cardModelId, qa, language});
            log.info("Store language for deck " + deckPath);
        } catch(Exception e) {
            log.error("Error", "Error storing language in MetaDB ", e);
        }
    }


    /**
     * Returns the language associated with the given deck, model and card model, for the given type.
     *
     * @param deckPath the deck for which to store the language association
     * @param modelId the model for which to store the language association
     * @param cardModelId the card model for which to store the language association
     * @param qa the part of the card for which to store the association, {@link #LANGUAGES_QA_QUESTION},
     *           {@link #LANGUAGES_QA_ANSWER}, or {@link #LANGUAGES_QA_UNDEFINED}
     * return the language associate with the type, as a two-characters, lowercase string, or the empty string if no
     *        association is defined
     */
    public static String getLanguage(Context context, String deckPath, long modelId, long cardModelId, int qa) {
        openDBIfClosed(context);
        String language = "";
        deckPath = stripQuotes(deckPath);
        ResultSet result = null;
        try {
            String query =
                    "SELECT language FROM languages "
                            + "WHERE deckpath = \'" + deckPath+ "\' "
                            + "AND modelid = " + modelId + " "
                            + "AND cardmodelid = " + cardModelId + " "
                            + "AND qa = " + qa + " "
                            + "LIMIT 1";
            result = mMetaDb.rawQuery(query, null);
            log.info("getLanguage: " + query);
            if (result.next()) {
                language = result.getString(1);
            }
        } catch(Exception e) {
            log.error("Error", "Error fetching language ", e);
        } finally {
            if (result != null) {
                result.close();
            }
        }
        return language;
    }


    /**
     * Resets all the language associates for a given deck.
     *
     * @param deckPath the deck for which to reset the language associations
     * @return whether an error occurred while resetting the language for the deck
     */
    public static boolean resetDeckLanguages(Context context, String deckPath) {
        openDBIfClosed(context);
        deckPath = stripQuotes(deckPath);
        try {
            mMetaDb.execSQL("DELETE FROM languages WHERE deckpath = \'" + deckPath + "\';");
            log.info("Resetting language assignment for deck " + deckPath);
            return true;
        } catch(Exception e) {
            log.error("Error", "Error resetting deck language", e);
        }
        return false;
    }


    /**
     * Returns the state of the whiteboard for the given deck.
     *
     * @param deckPath the deck for which to retrieve the whiteboard state
     * @return 1 if the whiteboard should be shown, 0 otherwise
     */
    public static int getWhiteboardState(Context context, String deckPath) {
        openDBIfClosed(context);
        ResultSet result = null;
        try {
            result = mMetaDb.rawQuery("SELECT state FROM whiteboardState"
                    + " WHERE deckpath = \'" + stripQuotes(deckPath) + "\'", null);
            if (result.next()) {
                return result.getInt(1);
            } else {
                return 0;
            }
        } catch(Exception e) {
            log.error("Error", "Error retrieving whiteboard state from MetaDB ", e);
            return 0;
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }


    /**
     * Stores the state of the whiteboard for a given deck.
     *
     * @param deckPath the deck for which to store the whiteboard state
     * @param state 1 if the whiteboard should be shown, 0 otherwise
     */
    public static void storeWhiteboardState(Context context, String deckPath, int state) {
        openDBIfClosed(context);
        deckPath = stripQuotes(deckPath);
        ResultSet result = null;
        try {
            result = mMetaDb.rawQuery("SELECT _id FROM whiteboardState"
                    + " WHERE deckpath = \'" + deckPath + "\'", null);
            if (result.next()) {
                mMetaDb.execSQL("UPDATE whiteboardState "
                        + "SET deckpath=\'" + deckPath + "\', "
                        + "state=" + Integer.toString(state) + " "
                        + "WHERE _id=" + result.getString(1) + ";");
                log.info("Store whiteboard state (" + state + ") for deck " + deckPath);
            } else {
                mMetaDb.execSQL("INSERT INTO whiteboardState (deckpath, state) VALUES (?, ?)",
                        new Object[]{deckPath, state});
                log.info("Store whiteboard state (" + state + ") for deck " + deckPath);
            }
        } catch(Exception e) {
            log.error("Error", "Error storing whiteboard state in MetaDB ", e);
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }


    /**
     * Returns a custom dictionary associated to a deck
     *
     * @param deckPath the deck for which a custom dictionary should be retrieved
     * @return integer number of dictionary, -1 if not set (standard dictionary will be used)
     */
    public static int getLookupDictionary(Context context, String deckPath) {
        openDBIfClosed(context);
        ResultSet result = null;
        try {
            result = mMetaDb.rawQuery("SELECT dictionary FROM customDictionary"
                    + " WHERE deckpath = \'" + stripQuotes(deckPath) + "\'", null);
            if (result.next()) {
                return result.getInt(1);
            } else {
                return -1;
            }
        } catch(Exception e) {
            log.error("Error", "Error retrieving custom dictionary from MetaDB ", e);
            return -1;
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }


    /**
     * Stores a custom dictionary for a given deck.
     *
     * @param deckPath the deck for which a custom dictionary should be retrieved
     * @param dictionary integer number of dictionary, -1 if not set (standard dictionary will be used)
     */
    public static void storeLookupDictionary(Context context, String deckPath, int dictionary) {
        openDBIfClosed(context);
        deckPath = stripQuotes(deckPath);
        ResultSet result = null;
        try {
            result = mMetaDb.rawQuery("SELECT _id FROM customDictionary"
                    + " WHERE deckpath = \'" + deckPath + "\'", null);
            if (result.next()) {
                mMetaDb.execSQL("UPDATE customDictionary "
                        + "SET deckpath=\'" + deckPath + "\', "
                        + "dictionary=" + Integer.toString(dictionary) + " "
                        + "WHERE _id=" + result.getString(1) + ";");
                log.info("Store custom dictionary (" + dictionary + ") for deck " + deckPath);
            } else {
                mMetaDb.execSQL("INSERT INTO customDictionary (deckpath, dictionary) VALUES (?, ?)",
                        new Object[]{deckPath, dictionary});
                log.info("Store custom dictionary (" + dictionary + ") for deck " + deckPath);
            }
        } catch(Exception e) {
            log.error("Error", "Error storing custom dictionary to MetaDB ", e);
        } finally {
            if (result != null) {
                result.close();
            }
        }
    }


    /**
     * Return the current status of the widget.
     *
     * @return an array of {@link DeckStatus} objects, each representing the status of one of the known decks
     */
    public static DeckStatus[] getWidgetStatus(Context context) {
        openDBIfClosed(context);
        ResultSet result = null;
        try {
            result = mMetaDb.query("widgetStatus",
                    new String[]{"deckPath", "deckName", "newCards", "dueCards", "failedCards", "eta", "time"},
                    null, null, null, null, "deckName");
            int count = result.getCount();
            DeckStatus[] decks = new DeckStatus[count];
            for(int index = 0; index < count; ++index) {
                if (!result.next()) {
                    throw new SQLException("cursor count was incorrect");
                }
                decks[index] = new DeckStatus(
                        result.getString(result.getColumnIndexOrThrow("deckPath")),
                        result.getString(result.getColumnIndexOrThrow("deckName")),
                        result.getInt(result.getColumnIndexOrThrow("newCards")),
                        result.getInt(result.getColumnIndexOrThrow("dueCards")),
                        result.getInt(result.getColumnIndexOrThrow("failedCards")),
                        result.getInt(result.getColumnIndexOrThrow("eta")),
                		result.getInt(result.getColumnIndexOrThrow("time")));
            }
            return decks;
        } catch (SQLException e) {
            log.error("Error while querying widgetStatus", e);
        } finally {
            if (result != null) {
                result.close();
            }
        }
        return new DeckStatus[0];
    }


    /**
     * Return the current status of the widget.
     *
     * @return an int array, containing due, time, eta, currentDeckdue
     */
    public static int[] getWidgetSmallStatus(Context context) {
        openDBIfClosed(context);
        ResultSet result = null;
        int due = 0;
        int eta = 0;
        int time = 0;
        boolean noDeck = true;
        try {
            result = mMetaDb.query("widgetStatus",
                    new String[]{"dueCards", "failedCards", "newCards", "time", "eta"},
                    null, null, null, null, null);
            while (result.next()) {
            	noDeck = false;
            	int d = result.getInt(1) + result.getInt(2) + result.getInt(3);
            	due += d;
            	time += result.getInt(4);
            	eta += result.getInt(5);
            }
        } catch (SQLException e) {
            log.error("Error while querying widgetStatus", e);
        } finally {
            if (result != null) {
                result.close();
            }
        }
        return new int[]{noDeck ? -1 : due, time, eta};
    }


    public static int getNotificationStatus(Context context) {
        openDBIfClosed(context);
        ResultSet result = null;
        int due = 0;
        try {
            result = mMetaDb.query("widgetStatus",
                    new String[]{"dueCards", "failedCards", "newCards"},
                    null, null, null, null, null);
            while (result.next()) {
            	due += result.getInt(1) + result.getInt(2) + result.getInt(3);
            }
        } catch (SQLException e) {
            log.error("Error while querying widgetStatus", e);
        } finally {
            if (result != null) {
                result.close();
            }
        }
        return due;
    }


    /**
     * Stores the current state of the widget.
     * <p>
     * It replaces any stored state for the widget.
     *
     * @param decks an array of {@link DeckStatus} objects, one for each of the know decks.
     */
    public static void storeWidgetStatus(Context context, DeckStatus[] decks) {
        openDBIfClosed(context);
        try {
            mMetaDb.beginTransaction();
            // First clear all the existing content.
            mMetaDb.execSQL("DELETE FROM widgetStatus");
            for (DeckStatus deck : decks) {
                mMetaDb.execSQL("INSERT INTO widgetStatus(deckPath, deckName, newCards, dueCards, failedCards, eta, time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        new Object[]{deck.mDeckPath, deck.mDeckName, deck.mNewCards, deck.mDueCards, deck.mFailedCards, deck.mEta, deck.mTime}
                        );
            }
            mMetaDb.setTransactionSuccessful();
            mMetaDb.endTransaction();
        } catch (IllegalStateException e) {
            log.error("MetaDB.storeWidgetStatus: failed", e);
        } catch (SQLException e) {
            log.error("MetaDB.storeWidgetStatus: failed", e);
            closeDB();
            log.info("Trying to reset Widget: " + resetWidget(context));
        }
    }


    public static ArrayList<HashMap<String, String>> getIntentInformation(Context context) {
        openDBIfClosed(context);
        ResultSet result = null;
        ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        try {
            result = mMetaDb.query("intentInformation",
                    new String[]{"id", "source", "target"},
                    null, null, null, null, "id");
            while (result.next()) {
            	HashMap<String, String> item = new HashMap<String, String>();
            	item.put("id", Integer.toString(result.getInt(1)));
            	item.put("source", result.getString(2));
            	item.put("target", result.getString(3));
            	list.add(item);
            }
        } catch (SQLException e) {
            log.error("Error while querying intentInformation", e);
        } finally {
            if (result != null) {
                result.close();
            }
        }
        return list;
    }


    public static void saveIntentInformation(Context context, String source, String target) {
        openDBIfClosed(context);
        try {
            mMetaDb.execSQL("INSERT INTO intentInformation (source, target) "
                            + " VALUES (?, ?);",
                            new Object[]{source, target});
            log.info("Store intentInformation: " + source + " - " + target);
        } catch(Exception e) {
            log.error("Error", "Error storing intentInformation in MetaDB ", e);
        }
    }


    public static boolean removeIntentInformation(Context context, String id) {
        if (mMetaDb == null || !mMetaDb.isOpen()) {
            openDB(context);
        }
        try {
            log.info("Deleting intent information " + id);
            mMetaDb.execSQL("DELETE FROM intentInformation WHERE id = " + id + ";");
            return true;
        } catch(Exception e) {
            log.error("Error", "Error deleting intentInformation " + id + ": ", e);
        }
        return false;
    }
}
