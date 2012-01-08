/***************************************************************************************
 * Copyright (c) 2009 Edu Zamora <edu.zasu@gmail.com>                                   *
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

package com.ichi2.anki.service;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ichi2.anki.AnkiDroidProxy;
import com.ichi2.anki.Utils;
import com.ichi2.anki.db.AnkiDatabaseManager;
import com.ichi2.anki.db.AnkiDb;
import com.ichi2.anki.model.Deck;
import com.ichi2.anki.model.Stats;

public class SyncClient {

	private static Logger log = LoggerFactory.getLogger(SyncClient.class);
	
    private enum Keys {
        models, facts, cards, media
    };

    /**
     * Constants used on the multipart message
     */
    private static final String MIME_BOUNDARY = "Anki-sync-boundary";
    private static final String END = "\r\n";
    private static final String TWO_HYPHENS = "--";

    private Deck mDeck;
    private AnkiDroidProxy mServer;
    private double mLocalTime;
    private double mRemoteTime;


    public SyncClient(Deck deck) {
        mDeck = deck;
        mServer = null;
        mLocalTime = 0;
        mRemoteTime = 0;
    }


    public AnkiDroidProxy getServer() {
        return mServer;
    }


    public void setServer(AnkiDroidProxy server) {
        mServer = server;
    }

    public double getRemoteTime() {
        return mRemoteTime;
    }

    public void setRemoteTime(double time) {
        mRemoteTime = time;
    }

    public double getLocalTime() {
        return mLocalTime;
    }

    public void setLocalTime(double time) {
        mLocalTime = time;
    }

    public void setDeck(Deck deck) {
        mDeck = deck;
    }


    /**
     * Anki Desktop -> libanki/anki/sync.py, prepareSync
     *
     * @return
     */
    public boolean prepareSync(double timediff) {
        log.info("prepareSync = " + String.format(Utils.ENGLISH_LOCALE, "%f", mDeck.getLastSync()));

        mLocalTime = mDeck.getModified();
        mRemoteTime = mServer.modified();

        log.info("localTime = " + mLocalTime);
        log.info("remoteTime = " + mRemoteTime);

        if (mLocalTime == mRemoteTime) {
            return false;
        }

        double l = mDeck.getLastSync();
        log.info("lastSync local = " + l);
        double r = mServer.lastSync();
        log.info("lastSync remote = " + r);

        // Set lastSync to the lower of the two sides, and account for slow clocks & assume it took up to 10 seconds
        // for the reply to arrive
        mDeck.setLastSync(Math.min(l, r) - timediff - 10);

        return true;
    }


    public JSONArray summaries() throws JSONException {

        JSONArray summaries = new JSONArray();
        JSONObject sum = summary(mDeck.getLastSync());
        if (sum == null) {
            return null;
        }
        summaries.put(sum);
        sum = mServer.summary(mDeck.getLastSync());
        summaries.put(sum);
        if (sum == null) {
            return null;
        }

        return summaries;
    }


    /**
     * Anki Desktop -> libanki/anki/sync.py, SyncTools - summary
     *
     * @param lastSync
     * @throws JSONException
     */
    public JSONObject summary(double lastSync) throws JSONException {
        log.info("Summary Local");
        mDeck.setLastSync(lastSync);
        mDeck.commitToDB();

        AnkiDb ankiDB = mDeck.getDB();

        String lastSyncString = String.format(Utils.ENGLISH_LOCALE, "%f", lastSync);
        // Cards
        JSONArray cards = resultSetToJSONArray(ankiDB.rawQuery(
                "SELECT id, modified FROM cards WHERE modified > " + lastSyncString));
        // Cards - delcards
        JSONArray delcards = resultSetToJSONArray(ankiDB.rawQuery(
                "SELECT cardId, deletedTime FROM cardsDeleted WHERE deletedTime > " + lastSyncString));

        // Facts
        JSONArray facts = resultSetToJSONArray(ankiDB.rawQuery(
                "SELECT id, modified FROM facts WHERE modified > " + lastSyncString));
        // Facts - delfacts
        JSONArray delfacts = resultSetToJSONArray(ankiDB.rawQuery(
                "SELECT factId, deletedTime FROM factsDeleted WHERE deletedTime > " + lastSyncString));

        // Models
        JSONArray models = resultSetToJSONArray(ankiDB.rawQuery(
                "SELECT id, modified FROM models WHERE modified > " + lastSyncString));
        // Models - delmodels
        JSONArray delmodels = resultSetToJSONArray(ankiDB.rawQuery(
                "SELECT modelId, deletedTime FROM modelsDeleted WHERE deletedTime > " + lastSyncString));

        // Media
        JSONArray media = resultSetToJSONArray(ankiDB.rawQuery(
                "SELECT id, created FROM media WHERE created > " + lastSyncString));
        // Media - delmedia
        JSONArray delmedia = resultSetToJSONArray(ankiDB.rawQuery(
                "SELECT mediaId, deletedTime FROM mediaDeleted WHERE deletedTime > " + lastSyncString));

        JSONObject summary = new JSONObject();
        try {
            summary.put("cards", cards);
            summary.put("delcards", delcards);
            summary.put("facts", facts);
            summary.put("delfacts", delfacts);
            summary.put("models", models);
            summary.put("delmodels", delmodels);
            summary.put("media", media);
            summary.put("delmedia", delmedia);
        } catch (JSONException e) {
            log.error("SyncClient.summary - JSONException = " + e.getMessage());
            return null;
        }

        log.info("Summary Local = ");
        Utils.printJSONObject(summary, false);

        return summary;
    }


    private JSONArray resultSetToJSONArray(ResultSet result) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        try {
			while (result.next()) {
			    JSONArray element = new JSONArray();

			    element.put(result.getLong(0));
			    element.put(result.getDouble(1));
			    jsonArray.put(element);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (result != null) {
				try {
					result.close();
				} catch (SQLException e) {
				}
			}
		}

        return jsonArray;
    }


    /**
     * Anki Desktop -> libanki/anki/sync.py, SyncTools - genPayload
     * @throws JSONException
     */
    public JSONObject genPayload(JSONArray summaries) throws JSONException {
        // log.info("genPayload");
        // Ensure global stats are available (queue may not be built)
        preSyncRefresh();

        JSONObject payload = new JSONObject();

        Keys[] keys = Keys.values();

        for (int i = 0; i < keys.length; i++) {
            // log.info("Key " + keys[i].name());
            String key = keys[i].name();
            // Handle models, facts, cards and media
            JSONArray diff = diffSummary((JSONObject) summaries.get(0), (JSONObject) summaries.get(1), key);
            payload.put("added-" + key, getObjsFromKey((JSONArray) diff.get(0), key));
            payload.put("deleted-" + key, diff.get(1));
            payload.put("missing-" + key, diff.get(2));
            deleteObjsFromKey((JSONArray) diff.get(3), key);
        }

        // If the last modified deck was the local one, handle the remainder
        if (mLocalTime > mRemoteTime) {

            payload.put("stats", bundleStats());
            payload.put("history", bundleHistory());
            payload.put("sources", bundleSources());
            // Finally, set new lastSync and bundle the deck info
            payload.put("deck", bundleDeck());
        }

        log.info("Payload =");
        Utils.printJSONObject(payload, true); //XXX: Why writeToFile = true?

        return payload;
    }


    /**
     * Anki Desktop -> libanki/anki/sync.py, SyncTools - payloadChanges
     * @throws JSONException
     */
    /*
    private Object[] payloadChanges(JSONObject payload) throws JSONException {
        Object[] h = new Object[8];

        h[0] = payload.getJSONObject("added-facts").getJSONArray("facts").length();
        h[1] = payload.getJSONArray("missing-facts").length();
        h[2] = payload.getJSONArray("added-cards").length();
        h[3] = payload.getJSONArray("missing-cards").length();
        h[4] = payload.getJSONArray("added-models").length();
        h[5] = payload.getJSONArray("missing-models").length();

        if (mLocalTime > mRemoteTime) {
            h[6] = "all";
            h[7] = 0;
        } else {
            h[6] = 0;
            h[7] = "all";
        }
        return h;
    } */

    /* Unsued
    public String payloadChangeReport(JSONObject payload) throws JSONException {
        return AnkiDroidApp.getAppResources().getString(R.string.change_report_format, payloadChanges(payload));
    } */


    public void applyPayloadReply(JSONObject payloadReply) throws JSONException {
        log.info("applyPayloadReply");
        Keys[] keys = Keys.values();

        for (int i = 0; i < keys.length; i++) {
            String key = keys[i].name();
            updateObjsFromKey(payloadReply, key);
        }

        if (!payloadReply.isNull("deck")) {
            updateDeck(payloadReply.getJSONObject("deck"));
            updateStats(payloadReply.getJSONObject("stats"));
            updateHistory(payloadReply.getJSONArray("history"));
            if (!payloadReply.isNull("sources")) {
                updateSources(payloadReply.getJSONArray("sources"));
            }
            mDeck.commitToDB();
        }

        mDeck.commitToDB();

        // Rebuild priorities on client

        // Get card ids
        JSONArray cards = payloadReply.getJSONArray("added-cards");
        int len = cards.length();
        long[] cardIds = new long[len];
        for (int i = 0; i < len; i++) {
            cardIds[i] = cards.getJSONArray(i).getLong(0);
        }
        mDeck.updateCardTags(cardIds);
        rebuildPriorities(cardIds);

        long missingFacts = missingFacts();
        if (missingFacts != 0l) {
            log.error("Facts missing after sync (" + missingFacts + " facts)!");
        }
        assert missingFacts == 0l;

    }

    private long missingFacts() {
        try {
            return mDeck.getDB().queryScalar("SELECT count() FROM cards WHERE factId NOT IN (SELECT id FROM facts)");
        } catch (Exception e) {
            return 0;
        }
    }


    /**
     * Anki Desktop -> libanki/anki/sync.py, SyncTools - preSyncRefresh
     */
    private void preSyncRefresh() {
        Stats.globalStats(mDeck);
    }


    private void rebuildPriorities(long[] cardIds) {
        rebuildPriorities(cardIds, null);
    }
    private void rebuildPriorities(long[] cardIds, String[] suspend) {
        //try {
            mDeck.updateAllPriorities(true, false);
            mDeck.updatePriorities(cardIds, suspend, false);
        //} catch (SQLException e) {
        //    log.error(TAG + " SQLException e = " + e.getMessage());
        //}
    }


    /**
     * Anki Desktop -> libanki/anki/sync.py, SyncTools - diffSummary
     * @throws JSONException
     */
    private JSONArray diffSummary(JSONObject summaryLocal, JSONObject summaryServer, String key) throws JSONException {
        JSONArray locallyEdited = new JSONArray();
        JSONArray locallyDeleted = new JSONArray();
        JSONArray remotelyEdited = new JSONArray();
        JSONArray remotelyDeleted = new JSONArray();

        log.info("\ndiffSummary - Key = " + key);
        log.info("\nSummary local = ");
        Utils.printJSONObject(summaryLocal, false);
        log.info("\nSummary server = ");
        Utils.printJSONObject(summaryServer, false);

        // Hash of all modified ids
        HashSet<Long> ids = new HashSet<Long>();

        // Build a hash (id item key, modification time) of the modifications on server (null -> deleted)
        HashMap<Long, Double> remoteMod = new HashMap<Long, Double>();
        putExistingItems(ids, remoteMod, summaryServer.getJSONArray(key));
        HashMap<Long, Double> rdeletedIds = putDeletedItems(ids, remoteMod, summaryServer.getJSONArray("del" + key));

        // Build a hash (id item, modification time) of the modifications on client (null -> deleted)
        HashMap<Long, Double> localMod = new HashMap<Long, Double>();
        putExistingItems(ids, localMod, summaryLocal.getJSONArray(key));
        HashMap<Long, Double> ldeletedIds = putDeletedItems(ids, localMod, summaryLocal.getJSONArray("del" + key));

        Iterator<Long> idsIterator = ids.iterator();
        while (idsIterator.hasNext()) {
            Long id = idsIterator.next();
            Double localModTime = localMod.get(id);
            Double remoteModTime = remoteMod.get(id);

            log.info("\nid = " + id + ", localModTime = " + localModTime + ", remoteModTime = " + remoteModTime);
            // Changed/Existing on both sides
            if (localModTime != null && remoteModTime != null) {
                log.info("localModTime not null AND remoteModTime not null");
                if (localModTime < remoteModTime) {
                    log.info("Remotely edited");
                    remotelyEdited.put(id);
                } else if (localModTime > remoteModTime) {
                    log.info("Locally edited");
                    locallyEdited.put(id);
                }
            }
            // If it's missing on server or newer here, sync
            else if (localModTime != null && remoteModTime == null) {
                log.info("localModTime not null AND remoteModTime null");
                if (!rdeletedIds.containsKey(id) || rdeletedIds.get(id) < localModTime) {
                    log.info("Locally edited");
                    locallyEdited.put(id);
                } else {
                    log.info("Remotely deleted");
                    remotelyDeleted.put(id);
                }
            }
            // If it's missing locally or newer there, sync
            else if (remoteModTime != null && localModTime == null) {
                log.info("remoteModTime not null AND localModTime null");
                if (!ldeletedIds.containsKey(id) || ldeletedIds.get(id) < remoteModTime) {
                    log.info("Remotely edited");
                    remotelyEdited.put(id);
                } else {
                    log.info("Locally deleted");
                    locallyDeleted.put(id);
                }
            }
            // Deleted or not modified in both sides
            else {
                log.info("localModTime null AND remoteModTime null");
                if (ldeletedIds.containsKey(id) && !rdeletedIds.containsKey(id)) {
                    log.info("Locally deleted");
                    locallyDeleted.put(id);
                } else if (rdeletedIds.containsKey(id) && !ldeletedIds.containsKey(id)) {
                    log.info("Remotely deleted");
                    remotelyDeleted.put(id);
                }
            }
        }

        JSONArray diff = new JSONArray();
        diff.put(locallyEdited);
        diff.put(locallyDeleted);
        diff.put(remotelyEdited);
        diff.put(remotelyDeleted);

        return diff;
    }


    private void putExistingItems(HashSet<Long> ids, HashMap<Long, Double> dictExistingItems, JSONArray existingItems) throws JSONException {
        int nbItems = existingItems.length();
        for (int i = 0; i < nbItems; i++) {
            JSONArray itemModified = existingItems.getJSONArray(i);
            Long idItem = itemModified.getLong(0);
            Double modTimeItem = itemModified.getDouble(1);
            dictExistingItems.put(idItem, modTimeItem);
            ids.add(idItem);
        }
    }


    private HashMap<Long, Double> putDeletedItems(HashSet<Long> ids, HashMap<Long, Double> dictDeletedItems,
            JSONArray deletedItems) throws JSONException {
        HashMap<Long, Double> deletedIds = new HashMap<Long, Double>();
        int nbItems = deletedItems.length();
        for (int i = 0; i < nbItems; i++) {
            JSONArray itemModified = deletedItems.getJSONArray(i);
            Long idItem = itemModified.getLong(0);
            Double modTimeItem = itemModified.getDouble(1);
            dictDeletedItems.put(idItem, null);
            deletedIds.put(idItem, modTimeItem);
            ids.add(idItem);
        }

        return deletedIds;
    }


    private Object getObjsFromKey(JSONArray ids, String key) throws JSONException {
        if ("models".equalsIgnoreCase(key)) {
            return getModels(ids);
        } else if ("facts".equalsIgnoreCase(key)) {
            return getFacts(ids);
        } else if ("cards".equalsIgnoreCase(key)) {
            return getCards(ids);
        } else if ("media".equalsIgnoreCase(key)) {
            return getMedia(ids);
        }

        return null;
    }


    private void deleteObjsFromKey(JSONArray ids, String key) throws JSONException {
        if ("models".equalsIgnoreCase(key)) {
            deleteModels(ids);
        } else if ("facts".equalsIgnoreCase(key)) {
            mDeck.deleteFacts(Utils.jsonArrayToListString(ids));
        } else if ("cards".equalsIgnoreCase(key)) {
            mDeck.deleteCards(Utils.jsonArrayToListString(ids));
        } else if ("media".equalsIgnoreCase(key)) {
            deleteMedia(ids);
        }
    }


    private void updateObjsFromKey(JSONObject payloadReply, String key) throws JSONException {
        if ("models".equalsIgnoreCase(key)) {
            log.info("updateModels");
            updateModels(payloadReply.getJSONArray("added-models"));
        } else if ("facts".equalsIgnoreCase(key)) {
            log.info("updateFacts");
            updateFacts(payloadReply.getJSONObject("added-facts"));
        } else if ("cards".equalsIgnoreCase(key)) {
            log.info("updateCards");
            updateCards(payloadReply.getJSONArray("added-cards"));
        } else if ("media".equalsIgnoreCase(key)) {
            log.info("updateMedia");
            updateMedia(payloadReply.getJSONArray("added-media"));
        }
    }


    /**
     * Models
     */

    // TODO: Include the case with updateModified
    /**
     * Anki Desktop -> libanki/anki/sync.py, SyncTools - getModels
     *
     * @param ids
     * @return
     * @throws JSONException
     */
    private JSONArray getModels(JSONArray ids) throws JSONException// , boolean updateModified)
    {
        JSONArray models = new JSONArray();

        int nbIds = ids.length();
        for (int i = 0; i < nbIds; i++) {
            models.put(bundleModel(ids.getLong(i)));
        }

        return models;
    }


    /**
     * Anki Desktop -> libanki/anki/sync.py, SyncTools - bundleModel
     *
     * @param id
     * @return
     * @throws JSONException
     */
    private JSONObject bundleModel(Long id) throws JSONException// , boolean updateModified
    {
        JSONObject model = new JSONObject();
        ResultSet result = null;
        try {
        	result = mDeck.getDB().rawQuery(
	                "SELECT * FROM models WHERE id = " + id);
	        
	        if (result.next()) {
	        	int i = 1;
	            model.put("id", result.getLong(i++));
	            model.put("deckId", result.getInt(i++));
	            model.put("created", result.getDouble(i++));
	            model.put("modified", result.getDouble(i++));
	            model.put("tags", result.getString(i++));
	            model.put("name", result.getString(i++));
	            model.put("description", result.getString(i++));
	            model.put("features", result.getDouble(i++));
	            model.put("spacing", result.getDouble(i++));
	            model.put("initialSpacing", result.getDouble(i++));
	            model.put("source", result.getInt(i++));
	            model.put("fieldModels", bundleFieldModels(id));
	            model.put("cardModels", bundleCardModels(id));
	        }
	        
        } catch (SQLException e) {
        	
        } finally {
        	if (result != null) {
        		try {
					result.close();
				} catch (SQLException e) {
				}
        	}
        }

        log.info("Model = ");
        Utils.printJSONObject(model, false);

        return model;
    }


    /**
     * Anki Desktop -> libanki/anki/sync.py, SyncTools - bundleFieldModel
     *
     * @param id
     * @return
     * @throws JSONException
     */
    private JSONArray bundleFieldModels(Long id) throws JSONException {
        JSONArray fieldModels = new JSONArray();

        ResultSet result = mDeck.getDB().rawQuery(
                "SELECT * FROM fieldModels WHERE modelId = " + id);
        try {
			while (result.next()) {
			    JSONObject fieldModel = new JSONObject();

			    int i = 1;
			    fieldModel.put("id", result.getLong(i++));
			    fieldModel.put("ordinal", result.getInt(i++));
			    fieldModel.put("modelId", result.getLong(i++));
			    fieldModel.put("name", result.getString(i++));
			    fieldModel.put("description", result.getString(i++));
			    fieldModel.put("features", result.getString(i++));
			    fieldModel.put("required", result.getString(i++));
			    fieldModel.put("unique", result.getString(i++));
			    fieldModel.put("numeric", result.getString(i++));
			    fieldModel.put("quizFontFamily", result.getString(i++));
			    fieldModel.put("quizFontSize", result.getInt(i++));
			    fieldModel.put("quizFontColour", result.getString(i++));
			    fieldModel.put("editFontFamily", result.getString(i++));
			    fieldModel.put("editFontSize", result.getInt(i++));

			    fieldModels.put(fieldModel);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (result != null) {
				try {
					result.close();
				} catch (SQLException e) {
				}
			}
		}
			
        return fieldModels;
    }


    private JSONArray bundleCardModels(Long id) throws JSONException {
        JSONArray cardModels = new JSONArray();

        ResultSet result = mDeck.getDB().rawQuery(
                "SELECT * FROM cardModels WHERE modelId = " + id);
        try {
			while (result.next()) {
			    JSONObject cardModel = new JSONObject();

			    int i = 1;
			    cardModel.put("id", result.getLong(i++));
			    cardModel.put("ordinal", result.getInt(i++));
			    cardModel.put("modelId", result.getLong(i++));
			    cardModel.put("name", result.getString(i++));
			    cardModel.put("description", result.getString(i++));
			    cardModel.put("active", result.getString(i++));
			    cardModel.put("qformat", result.getString(i++));
			    cardModel.put("aformat", result.getString(i++));
			    cardModel.put("lformat", result.getString(i++));
			    cardModel.put("qedformat", result.getString(i++));
			    cardModel.put("aedformat", result.getString(i++));
			    cardModel.put("questionInAnswer", result.getString(i++));
			    cardModel.put("questionFontFamily", result.getString(i++));
			    cardModel.put("questionFontSize ", result.getInt(i++));
			    cardModel.put("questionFontColour", result.getString(i++));
			    cardModel.put("questionAlign", result.getInt(i++));
			    cardModel.put("answerFontFamily", result.getString(i++));
			    cardModel.put("answerFontSize", result.getInt(i++));
			    cardModel.put("answerFontColour", result.getString(i++));
			    cardModel.put("answerAlign", result.getInt(i++));
			    cardModel.put("lastFontFamily", result.getString(i++));
			    cardModel.put("lastFontSize", result.getInt(i++));
			    cardModel.put("lastFontColour", result.getString(i++));
			    cardModel.put("editQuestionFontFamily", result.getString(i++));
			    cardModel.put("editQuestionFontSize", result.getInt(i++));
			    cardModel.put("editAnswerFontFamily", result.getString(i++));
			    cardModel.put("editAnswerFontSize", result.getInt(i++));
			    cardModel.put("allowEmptyAnswer", result.getString(i++));
			    cardModel.put("typeAnswer", result.getString(i++));

			    cardModels.put(cardModel);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (result != null) {
				try {
					result.close();
				} catch (SQLException e) {
				}
			}
		}
        

        return cardModels;
    }


    private void deleteModels(JSONArray ids) throws JSONException {
        log.info("deleteModels");
        int len = ids.length();
        for (int i = 0; i < len; i++) {
            mDeck.deleteModel(ids.getString(i));
        }
    }


    private void updateModels(JSONArray models) throws JSONException {
        ArrayList<String> insertedModelsIds = new ArrayList<String>();
        AnkiDb ankiDB = mDeck.getDB();

        String sql = "INSERT OR REPLACE INTO models"
                    + " (id, deckId, created, modified, tags, name, description, features, spacing, initialSpacing, source)"
                    + " VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement statement = ankiDB.compileStatement(sql);
        
        int len = models.length();
        for (int i = 0; i < len; i++) {
            JSONObject model = models.getJSONObject(i);

            // id
            String id = model.getString("id");
            try {
				statement.setString(1, id);
	            // deckId
	            statement.setLong(2, model.getLong("deckId"));
	            // created
	            statement.setDouble(3, model.getDouble("created"));
	            // modified
	            statement.setDouble(4, model.getDouble("modified"));
	            // tags
	            statement.setString(5, model.getString("tags"));
	            // name
	            statement.setString(6, model.getString("name"));
	            // description
	            statement.setString(7, model.getString("name"));
	            // features
	            statement.setString(8, model.getString("features"));
	            // spacing
	            statement.setDouble(9, model.getDouble("spacing"));
	            // initialSpacing
	            statement.setDouble(10, model.getDouble("initialSpacing"));
	            // source
	            statement.setLong(11, model.getLong("source"));

	            statement.execute();
	            
			} catch (SQLException e) {
				e.printStackTrace();
			}

            insertedModelsIds.add(id);

            mergeFieldModels(id, model.getJSONArray("fieldModels"));
            mergeCardModels(id, model.getJSONArray("cardModels"));
        }
        if (statement != null) {
        	try {
				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
        }

        // Delete inserted models from modelsDeleted
        ankiDB.execSQL("DELETE FROM modelsDeleted WHERE modelId IN " + Utils.ids2str(insertedModelsIds));
    }


    private void mergeFieldModels(String modelId, JSONArray fieldModels) throws JSONException {
        ArrayList<String> ids = new ArrayList<String>();

        String sql = "INSERT OR REPLACE INTO fieldModels VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement statement = mDeck.getDB().compileStatement(sql);
        int len = fieldModels.length();
        for (int i = 0; i < len; i++) {
            JSONObject fieldModel = fieldModels.getJSONObject(i);

            // id
            String id = fieldModel.getString("id");
            try {
				statement.setString(1, id);
				
	            // ordinal
	            statement.setString(2, fieldModel.getString("ordinal"));
	            // modelId
	            statement.setLong(3, fieldModel.getLong("modelId"));
	            // name
	            statement.setString(4, fieldModel.getString("name"));
	            // description
	            statement.setString(5, fieldModel.getString("description"));
	            // features
	            statement.setString(6, fieldModel.getString("features"));
	            // required
	            statement.setLong(7, Utils.booleanToInt(fieldModel.getBoolean("required")));
	            // unique
	            statement.setLong(8, Utils.booleanToInt(fieldModel.getBoolean("unique")));
	            // numeric
	            statement.setLong(9, Utils.booleanToInt(fieldModel.getBoolean("numeric")));
	            // quizFontFamily
	            if (fieldModel.isNull("quizFontFamily")) {
	                statement.setNull(10, Types.CHAR);
	            } else {
	                statement.setString(10, fieldModel.getString("quizFontFamily"));
	            }
	            // quizFontSize
	            if (fieldModel.isNull("quizFontSize")) {
	                statement.setNull(11, Types.CHAR);
	            } else {
	                statement.setString(11, fieldModel.getString("quizFontSize"));
	            }
	            // quizFontColour
	            if (fieldModel.isNull("quizFontColour")) {
	                statement.setNull(12, Types.CHAR);
	            } else {
	                statement.setString(12, fieldModel.getString("quizFontColour"));
	            }
	            // editFontFamily
	            if (fieldModel.isNull("editFontFamily")) {
	                statement.setNull(13, Types.CHAR);
	            } else {
	                statement.setString(13, fieldModel.getString("editFontFamily"));
	            }
	            // editFontSize
	            statement.setString(14, fieldModel.getString("editFontSize"));

	            statement.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}

            ids.add(id);
        }
        if (statement != null) {
        	try {
				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
        }

        // Delete field models that were not returned by the server
        ArrayList<String> fieldModelsIds = mDeck.getDB().queryColumn(String.class,
                "SELECT id FROM fieldModels WHERE modelId = " + modelId, 0);
        if (fieldModelsIds != null) {
            for (String fieldModelId : fieldModelsIds) {
                if (!ids.contains(fieldModelId)) {
                    mDeck.deleteFieldModel(modelId, fieldModelId);
                }
            }
        }
    }


    private void mergeCardModels(String modelId, JSONArray cardModels) throws JSONException {
        ArrayList<String> ids = new ArrayList<String>();

        String sql = "INSERT OR REPLACE INTO cardModels (id, ordinal, modelId, name, description, active, qformat, "
                + "aformat, lformat, qedformat, aedformat, questionInAnswer, questionFontFamily, questionFontSize, "
                + "questionFontColour, questionAlign, answerFontFamily, answerFontSize, answerFontColour, answerAlign, "
                + "lastFontFamily, lastFontSize, lastFontColour, editQuestionFontFamily, editQuestionFontSize, "
                + "editAnswerFontFamily, editAnswerFontSize, allowEmptyAnswer, typeAnswer) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement statement = mDeck.getDB().compileStatement(sql);
        int len = cardModels.length();
        for (int i = 0; i < len; i++) {
            JSONObject cardModel = cardModels.getJSONObject(i);

            // id
            String id = cardModel.getString("id");
            try {
				statement.setString(1, id);
	            // ordinal
	            statement.setString(2, cardModel.getString("ordinal"));
	            // modelId
	            statement.setLong(3, cardModel.getLong("modelId"));
	            // name
	            statement.setString(4, cardModel.getString("name"));
	            // description
	            statement.setString(5, cardModel.getString("description"));
	            // active
	            statement.setLong(6, Utils.booleanToInt(cardModel.getBoolean("active")));
	            // qformat
	            statement.setString(7, cardModel.getString("qformat"));
	            // aformat
	            statement.setString(8, cardModel.getString("aformat"));
	            // lformat
	            if (cardModel.isNull("lformat")) {
	                statement.setNull(9, Types.CHAR);
	            } else {
	                statement.setString(9, cardModel.getString("lformat"));
	            }
	            // qedformat
	            if (cardModel.isNull("qedformat")) {
	                statement.setNull(10, Types.CHAR);
	            } else {
	                statement.setString(10, cardModel.getString("qedformat"));
	            }
	            // aedformat
	            if (cardModel.isNull("aedformat")) {
	                statement.setNull(11, Types.CHAR);
	            } else {
	                statement.setString(11, cardModel.getString("aedformat"));
	            }
	            // questionInAnswer
	            statement.setLong(12, Utils.booleanToInt(cardModel.getBoolean("questionInAnswer")));
	            // questionFontFamily
	            statement.setString(13, cardModel.getString("questionFontFamily"));
	            // questionFontSize
	            statement.setString(14, cardModel.getString("questionFontSize"));
	            // questionFontColour
	            statement.setString(15, cardModel.getString("questionFontColour"));
	            // questionAlign
	            statement.setString(16, cardModel.getString("questionAlign"));
	            // answerFontFamily
	            statement.setString(17, cardModel.getString("answerFontFamily"));
	            // answerFontSize
	            statement.setString(18, cardModel.getString("answerFontSize"));
	            // answerFontColour
	            statement.setString(19, cardModel.getString("answerFontColour"));
	            // answerAlign
	            statement.setString(20, cardModel.getString("answerAlign"));
	            // lastFontFamily
	            statement.setString(21, cardModel.getString("lastFontFamily"));
	            // lastFontSize
	            statement.setString(22, cardModel.getString("lastFontSize"));
	            // lastFontColour
	            statement.setString(23, cardModel.getString("lastFontColour"));
	            // editQuestionFontFamily
	            if (cardModel.isNull("editQuestionFontFamily")) {
	                statement.setNull(24, Types.CHAR);
	            } else {
	                statement.setString(24, cardModel.getString("editQuestionFontFamily"));
	            }
	            // editQuestionFontSize
	            if (cardModel.isNull("editQuestionFontSize")) {
	                statement.setNull(25, Types.CHAR);
	            } else {
	                statement.setString(25, cardModel.getString("editQuestionFontSize"));
	            }
	            // editAnswerFontFamily
	            if (cardModel.isNull("editAnswerFontFamily")) {
	                statement.setNull(26, Types.CHAR);
	            } else {
	                statement.setString(26, cardModel.getString("editAnswerFontFamily"));
	            }
	            // editAnswerFontSize
	            if (cardModel.isNull("editAnswerFontSize")) {
	                statement.setNull(27, Types.CHAR);
	            } else {
	                statement.setString(27, cardModel.getString("editAnswerFontSize"));
	            }
	            // allowEmptyAnswer
	            if (cardModel.isNull("allowEmptyAnswer")) {
	                cardModel.put("allowEmptyAnswer", true);
	            }
	            statement.setLong(28, Utils.booleanToInt(cardModel.getBoolean("allowEmptyAnswer")));
	            // typeAnswer
	            statement.setString(29, cardModel.getString("typeAnswer"));

	            statement.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}


            ids.add(id);
        }
        if (statement != null) {
        	try {
				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
        }

        // Delete card models that were not returned by the server
        ArrayList<String> cardModelsIds = mDeck.getDB().queryColumn(String.class, "SELECT id FROM cardModels WHERE modelId = "
                + modelId, 0);
        if (cardModelsIds != null) {
            for (String cardModelId : cardModelsIds) {
                if (!ids.contains(cardModelId)) {
                    mDeck.deleteCardModel(modelId, cardModelId);
                }
            }
        }
    }


    /**
     * Facts
     */

    // TODO: Take into account the updateModified boolean (modified = time.time() or modified = "modified"... what does
    // exactly do that?)
    /**
     * Anki Desktop -> libanki/anki/sync.py, SyncTools - getFacts
     * @throws JSONException
     */
    private JSONObject getFacts(JSONArray ids) throws JSONException// , boolean updateModified)
    {
        log.info("getFacts");

        JSONObject facts = new JSONObject();

        JSONArray factsArray = new JSONArray();
        JSONArray fieldsArray = new JSONArray();

        int len = ids.length();
        for (int i = 0; i < len; i++) {
            Long id = ids.getLong(i);
            factsArray.put(getFact(id));
            putFields(fieldsArray, id);
        }

        facts.put("facts", factsArray);
        facts.put("fields", fieldsArray);

        log.info("facts = ");
        Utils.printJSONObject(facts, false);

        return facts;
    }


    private JSONArray getFact(Long id) throws JSONException {
        JSONArray fact = new JSONArray();

        // TODO: Take into account the updateModified boolean (modified = time.time() or modified = "modified"... what
        // does exactly do that?)
        ResultSet result = mDeck.getDB()
                .rawQuery("SELECT id, modelId, created, modified, tags, spaceUntil, lastCardId FROM facts WHERE id = "
                        + id);
        try {
			if (result.next()) {
				int i = 1;
			    fact.put(result.getLong(i++));
			    fact.put(result.getLong(i++));
			    fact.put(result.getDouble(i++));
			    fact.put(result.getDouble(i++));
			    fact.put(result.getString(i++));
			    fact.put(result.getDouble(i++));
			    fact.put(result.getLong(i++));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (result != null) {
				try {
					result.close();
				} catch (SQLException e) {
				}	
			}
		}

        return fact;
    }


    private void putFields(JSONArray fields, Long id) {
        ResultSet result = mDeck.getDB().rawQuery(
                "SELECT * FROM fields WHERE factId = " + id);
        try {
			while (result.next()) {
			    JSONArray field = new JSONArray();

			    int i = 1;
			    field.put(result.getLong(i++));
			    field.put(result.getLong(i++));
			    field.put(result.getLong(i++));
			    field.put(result.getInt(i++));
			    field.put(result.getString(i++));

			    fields.put(field);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (result != null) {
				try {
					result.close();
				} catch (SQLException e) {
				}	
			}
		}
    }


    private void updateFacts(JSONObject factsDict) throws JSONException {
        AnkiDb ankiDB = mDeck.getDB();
        JSONArray facts = factsDict.getJSONArray("facts");
        int lenFacts = facts.length();

        if (lenFacts > 0) {
            JSONArray fields = factsDict.getJSONArray("fields");
            int lenFields = fields.length();

            // Grab fact ids
            // They will be used later to recalculate the count of facts and to delete them from DB
            ArrayList<String> factIds = new ArrayList<String>();
            for (int i = 0; i < lenFacts; i++) {
                factIds.add(facts.getJSONArray(i).getString(0));
            }
            String factIdsString = Utils.ids2str(factIds);

            // Update facts
            String sqlFact = "INSERT OR REPLACE INTO facts (id, modelId, created, modified, tags, spaceUntil, lastCardId)"
                            + " VALUES(?,?,?,?,?,?,?)";
            PreparedStatement statement = ankiDB.compileStatement(sqlFact);
            for (int i = 0; i < lenFacts; i++) {
                JSONArray fact = facts.getJSONArray(i);

                // id
                try {
					statement.setLong(1, fact.getLong(0));
	                // modelId
	                statement.setLong(2, fact.getLong(1));
	                // created
	                statement.setDouble(3, fact.getDouble(2));
	                // modified
	                statement.setDouble(4, fact.getDouble(3));
	                // tags
	                statement.setString(5, fact.getString(4));
	                // spaceUntil
	                if (fact.getString(5) == null) {
	                    statement.setString(6, "");
	                } else {
	                    statement.setString(6, fact.getString(5));
	                }
	                // lastCardId
	                if (!fact.isNull(6)) {
	                    statement.setLong(7, fact.getLong(6));
	                } else {
	                    statement.setNull(7, Types.BIGINT);
	                }

	                statement.executeUpdate();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


            }
            if (statement != null) {
            	try {
    				statement.close();
    			} catch (SQLException e) {
    				e.printStackTrace();
    			}
            }

            // Update fields (and delete first the local ones, since ids may have changed)
            ankiDB.execSQL("DELETE FROM fields WHERE factId IN " + factIdsString);

            String sqlFields = "INSERT INTO fields (id, factId, fieldModelId, ordinal, value) VALUES(?,?,?,?,?)";
            statement = ankiDB.compileStatement(sqlFields);
            for (int i = 0; i < lenFields; i++) {
                JSONArray field = fields.getJSONArray(i);

                // id
                try {
					statement.setLong(1, field.getLong(0));
	                // factId
	                statement.setLong(2, field.getLong(1));
	                // fieldModelId
	                statement.setLong(3, field.getLong(2));
	                // ordinal
	                statement.setString(4, field.getString(3));
	                // value
	                statement.setString(5, field.getString(4));

	                statement.executeUpdate();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            if (statement != null) {
            	try {
    				statement.close();
    			} catch (SQLException e) {
    				e.printStackTrace();
    			}
            }

            // Delete inserted facts from deleted
            ankiDB.execSQL("DELETE FROM factsDeleted WHERE factId IN " + factIdsString);
        }
    }


    /**
     * Cards
     */

    /**
     * Anki Desktop -> libanki/anki/sync.py, SyncTools - getCards
     * @throws JSONException
     */
    private JSONArray getCards(JSONArray ids) throws JSONException {
        JSONArray cards = new JSONArray();

        // SELECT id, factId, cardModelId, created, modified, tags, ordinal, priority, interval, lastInterval, due,
        // lastDue, factor,
        // firstAnswered, reps, successive, averageTime, reviewTime, youngEase0, youngEase1, youngEase2, youngEase3,
        // youngEase4,
        // matureEase0, matureEase1, matureEase2, matureEase3, matureEase4, yesCount, noCount, question, answer,
        // lastFactor, spaceUntil, relativeDelay,
        // type, combinedDue FROM cards WHERE id IN " + ids2str(ids)
        ResultSet result = mDeck.getDB().rawQuery(
                "SELECT * FROM cards WHERE id IN " + Utils.ids2str(ids));
        try {
			while (result.next()) {
			    JSONArray card = new JSONArray();

			    // id
			    card.put(result.getLong(1));
			    // factId
			    card.put(result.getLong(2));
			    // cardModelId
			    card.put(result.getLong(3));
			    // created
			    card.put(result.getDouble(4));
			    // modified
			    card.put(result.getDouble(5));
			    // tags
			    card.put(result.getString(6));
			    // ordinal
			    card.put(result.getInt(7));
			    // priority
			    card.put(result.getInt(10));
			    // interval
			    card.put(result.getDouble(11));
			    // lastInterval
			    card.put(result.getDouble(12));
			    // due
			    card.put(result.getDouble(13));
			    // lastDue
			    card.put(result.getDouble(14));
			    // factor
			    card.put(result.getDouble(15));
			    // firstAnswered
			    card.put(result.getDouble(17));
			    // reps
			    card.put(result.getString(18));
			    // successive
			    card.put(result.getInt(19));
			    // averageTime
			    card.put(result.getDouble(20));
			    // reviewTime
			    card.put(result.getDouble(21));
			    // youngEase0
			    card.put(result.getInt(22));
			    // youngEase1
			    card.put(result.getInt(23));
			    // youngEase2
			    card.put(result.getInt(24));
			    // youngEase3
			    card.put(result.getInt(25));
			    // youngEase4
			    card.put(result.getInt(26));
			    // matureEase0
			    card.put(result.getInt(27));
			    // matureEase1
			    card.put(result.getInt(28));
			    // matureEase2
			    card.put(result.getInt(29));
			    // matureEase3
			    card.put(result.getInt(30));
			    // matureEase4
			    card.put(result.getInt(31));
			    // yesCount
			    card.put(result.getInt(32));
			    // noCount
			    card.put(result.getInt(33));
			    // question
			    card.put(result.getString(8));
			    // answer
			    card.put(result.getString(9));
			    // lastFactor
			    card.put(result.getDouble(16));
			    // spaceUntil
			    card.put(result.getDouble(34));
			    // type
			    card.put(result.getInt(37));
			    // combinedDue
			    card.put(result.getDouble(38));
			    // relativeDelay
			    card.put(result.getInt(35));

			    cards.put(card);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (result != null) {
				try {
					result.close();
				} catch (SQLException e) {
				}
			}
		}
       

        return cards;
    }


    private void updateCards(JSONArray cards) throws JSONException {
        int len = cards.length();
        if (len > 0) {
            AnkiDb ankiDB = mDeck.getDB();
            ArrayList<String> ids = new ArrayList<String>();
            for (int i = 0; i < len; i++) {
                ids.add(cards.getJSONArray(i).getString(0));
            }
            String idsString = Utils.ids2str(ids);

            String sql = "INSERT OR REPLACE INTO cards (id, factId, cardModelId, created, modified, tags, ordinal, "
                    + "priority, interval, lastInterval, due, lastDue, factor, firstAnswered, reps, successive, "
                    + "averageTime, reviewTime, youngEase0, youngEase1, youngEase2, youngEase3, youngEase4, "
                    + "matureEase0, matureEase1, matureEase2, matureEase3, matureEase4, yesCount, noCount, question, "
                    + "answer, lastFactor, spaceUntil, type, combinedDue, relativeDelay, isDue) "
                    + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, 0)";
            PreparedStatement statement = ankiDB.compileStatement(sql);
            for (int i = 0; i < len; i++) {
                JSONArray card = cards.getJSONArray(i);

                // id
                try {
					statement.setLong(1, card.getLong(0));
	                // factId
	                statement.setLong(2, card.getLong(1));
	                // cardModelId
	                statement.setLong(3, card.getLong(2));
	                // created
	                statement.setDouble(4, card.getDouble(3));
	                // modified
	                statement.setDouble(5, card.getDouble(4));
	                // tags
	                statement.setString(6, card.getString(5));
	                // ordinal
	                statement.setLong(7, card.getInt(6));
	                // priority
	                statement.setLong(8, card.getInt(7));
	                // interval
	                statement.setDouble(9, card.getDouble(8));
	                // lastInterval
	                statement.setDouble(10, card.getDouble(9));
	                // due
	                statement.setDouble(11, card.getDouble(10));
	                // lastDue
	                statement.setDouble(12, card.getDouble(11));
	                // factor
	                statement.setDouble(13, card.getDouble(12));
	                // firstAnswered
	                statement.setDouble(14, card.getDouble(13));
	                // reps
	                statement.setLong(15, card.getInt(14));
	                // successive
	                statement.setLong(16, card.getInt(15));
	                // averageTime
	                statement.setDouble(17, card.getDouble(16));
	                // reviewTime
	                statement.setDouble(18, card.getDouble(17));
	                // youngEase0
	                statement.setLong(19, card.getInt(18));
	                // youngEase1
	                statement.setLong(20, card.getInt(19));
	                // youngEase2
	                statement.setLong(21, card.getInt(20));
	                // youngEase3
	                statement.setLong(22, card.getInt(21));
	                // youngEase4
	                statement.setLong(23, card.getInt(22));
	                // matureEase0
	                statement.setLong(24, card.getInt(23));
	                // matureEase1
	                statement.setLong(25, card.getInt(24));
	                // matureEase2
	                statement.setLong(26, card.getInt(25));
	                // matureEase3
	                statement.setLong(27, card.getInt(26));
	                // matureEase4
	                statement.setLong(28, card.getInt(27));
	                // yesCount
	                statement.setLong(29, card.getInt(28));
	                // noCount
	                statement.setLong(30, card.getInt(29));
	                // question
	                statement.setString(31, card.getString(30));
	                // answer
	                statement.setString(32, card.getString(31));
	                // lastFactor
	                statement.setDouble(33, card.getDouble(32));
	                // spaceUntil
	                statement.setDouble(34, card.getDouble(33));
	                // type
	                statement.setLong(35, card.getInt(34));
	                // combinedDue
	                statement.setDouble(36, card.getDouble(35));
	                // relativeDelay
	                statement.setString(37, genType(card));

	                statement.executeUpdate();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            if (statement != null) {
            	try {
    				statement.close();
    			} catch (SQLException e) {
    				e.printStackTrace();
    			}
            }

            ankiDB.execSQL("DELETE FROM cardsDeleted WHERE cardId IN " + idsString);
        }
    }
    private String genType(JSONArray row) throws JSONException {
        if (row.length() >= 37) {
            return row.getString(36);
        }
        if (row.getInt(15) != 0) {
            return "1";
        } else if (row.getInt(14) != 0) {
            return "0";
        }
        return "2";
    }


    /**
     * Media
     */

    /**
     * Anki Desktop -> libanki/anki/sync.py, SyncTools - getMedia
     * @throws JSONException
     */
    private JSONArray getMedia(JSONArray ids) throws JSONException {
        JSONArray media = new JSONArray();

        ResultSet result = mDeck.getDB().rawQuery(
                "SELECT id, filename, size, created, originalPath, description FROM media WHERE id IN "
                        + Utils.ids2str(ids));
        try {
			while (result.next()) {
			    JSONArray m = new JSONArray();

			    // id
			    m.put(result.getLong(1));
			    // filename
			    m.put(result.getString(2));
			    // size
			    m.put(result.getInt(3));
			    // created
			    m.put(result.getDouble(4));
			    // originalPath
			    m.put(result.getString(5));
			    // description
			    m.put(result.getString(6));

			    media.put(m);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (result != null) {
				try {
					result.close();
				} catch (SQLException e) {
				}
			}
		}
        

        return media;
    }


    private void deleteMedia(JSONArray ids) throws JSONException {
        log.info("deleteMedia");

        String idsString = Utils.ids2str(ids);

        // Get filenames
        // files below is never used, so it's commented out
        // ArrayList<String> files = mDeck.getDB().queryColumn(String.class, "SELECT filename FROM media WHERE id IN "
        //         + idsString, 0);

        // Note the media to delete (Insert the media to delete into mediaDeleted)
        double now = Utils.now();
        String sqlInsert = "INSERT INTO mediaDeleted SELECT id, " + String.format(Utils.ENGLISH_LOCALE, "%f", now)
                + " FROM media WHERE media.id = ?";
        PreparedStatement statement = mDeck.getDB().compileStatement(sqlInsert);
        int len = ids.length();
        for (int i = 0; i < len; i++) {
            log.info("Inserting media " + ids.getLong(i) + " into mediaDeleted");
            try {
				statement.setLong(1, ids.getLong(i));
				
				statement.executeUpdate();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
        }
        if (statement != null) {
        	try {
				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
        }

        // Delete media
        log.info("Deleting media in = " + idsString);
        mDeck.getDB().execSQL("DELETE FROM media WHERE id IN " + idsString);
    }


    void updateMedia(JSONArray media) throws JSONException {
        AnkiDb ankiDB = mDeck.getDB();
        ArrayList<String> mediaIds = new ArrayList<String>();

        String sql = "INSERT OR REPLACE INTO media (id, filename, size, created, originalPath, description) "
                    + "VALUES(?,?,?,?,?,?)";
        PreparedStatement statement = ankiDB.compileStatement(sql);
        int len = media.length();
        String filename = null;
        String sum = null;
        for (int i = 0; i < len; i++) {
            JSONArray m = media.getJSONArray(i);

            // Grab media ids, to delete them later
            String id = m.getString(0);
            mediaIds.add(id);

            // id
            try {
				statement.setString(1, id);
				
	            // filename
	            filename = m.getString(1);
	            statement.setString(2, filename);
	            // size
	            statement.setString(3, m.getString(2));
	            // created
	            statement.setDouble(4, m.getDouble(3));
	            // originalPath
	            sum = m.getString(4);
	            statement.setString(5, sum);
	            // description
	            statement.setString(6, m.getString(5));

	            statement.executeUpdate();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        if (statement != null) {
        	try {
				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
        }

        ankiDB.execSQL("DELETE FROM mediaDeleted WHERE mediaId IN " + Utils.ids2str(mediaIds));

    }


    /**
     * Deck/Stats/History/Sources
     * @throws JSONException
     */

    private JSONObject bundleDeck() throws JSONException {
        JSONObject bundledDeck = new JSONObject();

        // Ensure modified is not greater than server time
        if ((mServer != null) && (mServer.getTimestamp() != 0.0)) {
            mDeck.setModified(Math.min(mDeck.getModified(), mServer.getTimestamp()));
            log.info(String.format(Utils.ENGLISH_LOCALE, "Modified: %f", mDeck.getModified()));
        }
        // And ensure lastSync is greater than modified
        mDeck.setLastSync(Math.max(Utils.now(), mDeck.getModified() + 1));
        log.info(String.format(Utils.ENGLISH_LOCALE, "LastSync: %f", mDeck.getLastSync()));

        bundledDeck = mDeck.bundleJson(bundledDeck);

        // AnkiDroid Deck.java does not have:
        // css, forceMediaDir, lastSessionStart, lastTags, needLock,
        // progressHandlerCalled,
        // progressHandlerEnabled, revCardOrder, sessionStartReps, sessionStartTime,
        // tmpMediaDir

        // XXX: this implies that they are not synched toward the server, I guess (tested on 0.7).
        // However, the ones left are not persisted by libanki on the DB, so it's a libanki bug that they are sync'ed at all.

        // Our bundleDeck also doesn't need all those fields that store the scheduler Methods

        // Add meta information of the deck (deckVars table)
        JSONArray meta = new JSONArray();
        ResultSet result = mDeck.getDB().rawQuery(
                "SELECT * FROM deckVars");
        try {
			while (result.next()) {
			    JSONArray deckVar = new JSONArray();
			    deckVar.put(result.getString(1));
			    deckVar.put(result.getString(2));
			    meta.put(deckVar);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (result != null) {
				try {
					result.close();
				} catch (SQLException e) {
				}
			}
		}
        
        bundledDeck.put("meta", meta);

        log.info("Deck =");
        Utils.printJSONObject(bundledDeck, false);

        return bundledDeck;
    }


    private void updateDeck(JSONObject deckPayload) throws JSONException {
        JSONArray meta = deckPayload.getJSONArray("meta");

        // Update meta information
        String sqlMeta = "INSERT OR REPLACE INTO deckVars (key, value) VALUES(?,?)";
        PreparedStatement statement = mDeck.getDB()
                .compileStatement(sqlMeta);
        int lenMeta = meta.length();
        for (int i = 0; i < lenMeta; i++) {
            JSONArray deckVar = meta.getJSONArray(i);

            // key
            try {
				statement.setString(1, deckVar.getString(0));
	            // value
	            statement.setString(2, deckVar.getString(1));

	            statement.executeUpdate();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        if (statement != null) {
        	try {
				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
        }

        // Update deck
        mDeck.updateFromJson(deckPayload);
    }


    private JSONObject bundleStats() throws JSONException {
        log.info("bundleStats");

        JSONObject bundledStats = new JSONObject();

        // Get daily stats since the last day the deck was synchronized
        Date lastDay = new Date(java.lang.Math.max(0, (long) (mDeck.getLastSync() - 60 * 60 * 24) * 1000));
        log.info("lastDay = " + lastDay.toString());
        ArrayList<Long> ids = mDeck.getDB().queryColumn(Long.class,
                "SELECT id FROM stats WHERE type = 1 and day >= \"" + lastDay.toString() + "\"", 0);

        Stats stat = new Stats(mDeck);
        // Put global stats
        bundledStats.put("global", Stats.globalStats(mDeck).bundleJson());
        // Put daily stats
        JSONArray dailyStats = new JSONArray();
        if (ids != null) {
            for (Long id : ids) {
                // Update stat with the values of the stat with id ids.get(i)
                stat.fromDB(id);
                // Bundle this stat and add it to dailyStats
                dailyStats.put(stat.bundleJson());
            }
        }
        bundledStats.put("daily", dailyStats);

        log.info("Stats =");
        Utils.printJSONObject(bundledStats, false);

        return bundledStats;
    }


    private void updateStats(JSONObject stats) throws JSONException {
        // Update global stats
        Stats globalStats = Stats.globalStats(mDeck);
        globalStats.updateFromJson(stats.getJSONObject("global"));

        // Update daily stats
        Stats stat = new Stats(mDeck);
        JSONArray remoteDailyStats = stats.getJSONArray("daily");
        int len = remoteDailyStats.length();
        for (int i = 0; i < len; i++) {
            // Get a specific daily stat
            JSONObject remoteStat = remoteDailyStats.getJSONObject(i);
            Date dailyStatDate = Utils.ordinalToDate(remoteStat.getInt("day"));

            // If exists a statistic for this day, get it
            Long id = mDeck.getDB().queryScalar(
                    "SELECT id FROM stats WHERE type = 1 AND day = \"" + dailyStatDate.toString() + "\"");
            if (id == -1) {
            	stat.create(Stats.STATS_DAY, dailyStatDate);
            } else {
            	stat.fromDB(id);
            }

            // Update daily stat
            stat.updateFromJson(remoteStat);
        }
    }


    private JSONArray bundleHistory() throws JSONException {
        double delay = 0.0;
        JSONArray bundledHistory = new JSONArray();
        ResultSet result = mDeck.getDB().rawQuery(
                        "SELECT cardId, time, lastInterval, nextInterval, ease, delay, lastFactor, nextFactor, reps, "
                        + "thinkingTime, yesCount, noCount FROM reviewHistory "
                        + "WHERE time > " + String.format(Utils.ENGLISH_LOCALE, "%f", mDeck.getLastSync()));
        try {
			while (result.next()) {
			    JSONArray review = new JSONArray();

			    int i = 1;
			    // cardId
			    review.put(0, (long)result.getLong(i++));
			    // time
			    review.put(1, (double)result.getDouble(i++));
			    // lastInterval
			    review.put(2, (double)result.getDouble(i++));
			    // nextInterval
			    review.put(3, (double)result.getDouble(i++));
			    // ease
			    review.put(4, (int)result.getInt(i++));
			    // delay
			    delay = result.getDouble(i++);
			    Number num = Double.valueOf(delay);
			    log.debug(String.format(Utils.ENGLISH_LOCALE, "issue 372 2: %.18f %s %s", delay, num.toString(), review.toString()));
			    review.put(5, delay);
			    log.debug(String.format(Utils.ENGLISH_LOCALE, "issue 372 3: %.18f %s %s", review.getDouble(5), num.toString(), review.toString()));
			    // lastFactor
			    review.put(6, (double)result.getDouble(i++));
			    log.debug(String.format(Utils.ENGLISH_LOCALE, "issue 372 4: %.18f %s %s", review.getDouble(5), num.toString(), review.toString()));
			    // nextFactor
			    review.put(7, (double)result.getDouble(i++));
			    // reps
			    review.put(8, (double)result.getDouble(i++));
			    // thinkingTime
			    review.put(9, (double)result.getDouble(i++));
			    // yesCount
			    review.put(10, (double)result.getDouble(i++));
			    // noCount
			    review.put(11, (double)result.getDouble(i++));

			    log.debug(String.format(Utils.ENGLISH_LOCALE, "issue 372 complete row: %.18f %.18f %s", delay, review.getDouble(5), review.toString()));
			    bundledHistory.put(review);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (result != null) {
				try {
					result.close();
				} catch (SQLException e) {
				}
			}
		}
        

        log.info("Last sync = " + String.format(Utils.ENGLISH_LOCALE, "%f", mDeck.getLastSync()));
        log.info("Bundled history = " + bundledHistory.toString());
        return bundledHistory;
    }


    private void updateHistory(JSONArray history) throws JSONException {
        String sql = "INSERT OR IGNORE INTO reviewHistory (cardId, time, lastInterval, nextInterval, ease, delay, "
                    + "lastFactor, nextFactor, reps, thinkingTime, yesCount, noCount) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        PreparedStatement statement = mDeck.getDB().compileStatement(sql);
        int len = history.length();
        for (int i = 0; i < len; i++) {
            JSONArray h = history.getJSONArray(i);

            // cardId
            try {
				statement.setLong(1, h.getLong(0));
	            // time
	            statement.setDouble(2, h.getDouble(1));
	            // lastInterval
	            statement.setDouble(3, h.getDouble(2));
	            // nextInterval
	            statement.setDouble(4, h.getDouble(3));
	            // ease
	            statement.setString(5, h.getString(4));
	            // delay
	            statement.setDouble(6, h.getDouble(5));
	            // lastFactor
	            statement.setDouble(7, h.getDouble(6));
	            // nextFactor
	            statement.setDouble(8, h.getDouble(7));
	            // reps
	            statement.setDouble(9, h.getDouble(8));
	            // thinkingTime
	            statement.setDouble(10, h.getDouble(9));
	            // yesCount
	            statement.setDouble(11, h.getDouble(10));
	            // noCount
	            statement.setDouble(12, h.getDouble(11));

	            statement.executeUpdate();
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        if (statement != null) {
        	try {
				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
        }
    }


    private JSONArray bundleSources() throws JSONException {
        JSONArray bundledSources = new JSONArray();

        ResultSet result = mDeck.getDB().rawQuery(
                "SELECT * FROM sources");
        try {
			while (result.next()) {
			    JSONArray source = new JSONArray();

			    // id
			    source.put(result.getLong(1));
			    // name
			    source.put(result.getString(2));
			    // created
			    source.put(result.getDouble(3));
			    // lastSync
			    source.put(result.getDouble(4));
			    // syncPeriod
			    source.put(result.getInt(5));

			    bundledSources.put(source);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (result != null) {
		        try {
					result.close();
				} catch (SQLException e) {
				}		
			}
		}

        log.info("Bundled sources = " + bundledSources);
        return bundledSources;
    }


    private void updateSources(JSONArray sources) throws JSONException {
        String sql = "INSERT OR REPLACE INTO sources VALUES(?,?,?,?,?)";
        PreparedStatement statement = mDeck.getDB().compileStatement(sql);
        int len = sources.length();
        for (int i = 0; i < len; i++) {
            JSONArray source = sources.getJSONArray(i);
            try {
				statement.setLong(1, source.getLong(0));
				statement.setString(2, source.getString(1));
				statement.setDouble(3, source.getDouble(2));
		        statement.setDouble(4, source.getDouble(3));
		        statement.setString(5, source.getString(4));
		        statement.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
           
        }
        if (statement != null) {
        	try {
				statement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
        }
    }


    /**
     * Full sync
     */

    /**
     * Anki Desktop -> libanki/anki/sync.py, SyncTools - needFullSync
     *
     * @param sums
     * @return
     * @throws JSONException
     */
    public boolean needFullSync(JSONArray sums) throws JSONException {
        log.info("needFullSync - lastSync = " + mDeck.getLastSync());

        if (mDeck.getLastSync() <= 0) {
            log.info("deck.lastSync <= 0");
            return true;
        }

        int len = sums.length();
        for (int i = 0; i < len; i++) {

            JSONObject summary = sums.getJSONObject(i);
            @SuppressWarnings("unchecked") Iterator<String> keys = (Iterator<String>) summary.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONArray l = (JSONArray) summary.get(key);
                log.info("Key " + key + ", length = " + l.length());
                if (l.length() > 500) {
                    log.info("Length of key > 500");
                    return true;
                }
            }
        }

        AnkiDb ankiDB = mDeck.getDB();

        if (ankiDB.queryScalar("SELECT count() FROM reviewHistory WHERE time > " + mDeck.getLastSync()) > 500) {
            log.info("reviewHistory since lastSync > 500");
            return true;
        }
        Date lastDay = new Date(java.lang.Math.max(0, (long) (mDeck.getLastSync() - 60 * 60 * 24) * 1000));

        log.info("lastDay = " + lastDay.toString() + ", lastDayInMillis = " + lastDay.getTime());

        log.info("Count stats = " + ankiDB.queryScalar("SELECT count() FROM stats WHERE day >= \"" + lastDay.toString() + "\""));
        if (ankiDB.queryScalar("SELECT count() FROM stats WHERE day >= \"" + lastDay.toString() + "\"") > 100) {
            log.info("stats since lastDay > 100");
            return true;
        }

        return false;
    }


    public String prepareFullSync() {
        // Ensure modified is not greater than server time
        mDeck.setModified(Math.min(mDeck.getModified(), mServer.getTimestamp()));
        mDeck.commitToDB();
        // The deck is closed after the full sync is completed

        if (mLocalTime > mRemoteTime) {
            return "fromLocal";
        } else {
            return "fromServer";
        }
    }


    public static HashMap<String, String> fullSyncFromLocal(String password, String username, Deck deck, String deckName) {
        HashMap<String, String> result = new HashMap<String, String>();
        Throwable exc = null;
        try {
            log.info("Fullup");
            // We need to write the output to a temporary file, so that FileEntity knows the length
            String tmpPath = (new File(deck.getDeckPath())).getParent();
            File tmpFile = new File(tmpPath + "/fulluploadPayload.tmp");
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
            log.info("Writing temporary payload file...");
            tmpFile.createNewFile();
            DataOutputStream tmp = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFile)));
            tmp.writeBytes(TWO_HYPHENS + MIME_BOUNDARY + END);
            tmp.writeBytes("Content-Disposition: form-data; name=\"p\"" + END + END + password + END);
            tmp.writeBytes(TWO_HYPHENS + MIME_BOUNDARY + END);
            tmp.writeBytes("Content-Disposition: form-data; name=\"u\"" + END + END + username + END);
            tmp.writeBytes(TWO_HYPHENS + MIME_BOUNDARY + END);
            tmp.writeBytes("Content-Disposition: form-data; name=\"d\"" + END + END);
            tmp.write(deckName.getBytes("UTF-8"));
            tmp.writeBytes(END);
            tmp.writeBytes(TWO_HYPHENS + MIME_BOUNDARY + END);
            tmp.writeBytes("Content-Disposition: form-data; name=\"deck\"; filename=\"deck\"" + END);
            tmp.writeBytes("Content-Type: application/octet-stream" + END);
            tmp.writeBytes(END);

            String deckPath = deck.getDeckPath();
            FileInputStream fStream = new FileInputStream(deckPath);
            byte[] buffer = new byte[Utils.CHUNK_SIZE];
            int length = -1;
            Deflater deflater = new Deflater(Deflater.BEST_SPEED);
            DeflaterOutputStream dos = new DeflaterOutputStream(tmp, deflater);
            while ((length = fStream.read(buffer)) != -1) {
                dos.write(buffer, 0, length);
                log.info("Length = " + length);
            }
            dos.finish();
            fStream.close();

            tmp.writeBytes(END);
            tmp.writeBytes(TWO_HYPHENS + MIME_BOUNDARY + TWO_HYPHENS + END + END);
            tmp.flush();
            tmp.close();
            log.info("Payload file ready, size: " + tmpFile.length());

            HttpPost httpPost = new HttpPost(AnkiDroidProxy.SYNC_URL + "fullup?v=" +
                    URLEncoder.encode(AnkiDroidProxy.SYNC_VERSION, "UTF-8"));
            httpPost.setHeader("Content-type", "multipart/form-data; boundary=" + MIME_BOUNDARY);
            httpPost.addHeader("Host", AnkiDroidProxy.SYNC_HOST);
            httpPost.setEntity(new FileEntity(tmpFile, "application/octet-stream"));
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpResponse resp = httpClient.execute(httpPost);

            // Ensure we got the HTTP 200 response code
            String response = Utils.convertStreamToString(resp.getEntity().getContent());
            int responseCode = resp.getStatusLine().getStatusCode();
            log.info("Response code = " + responseCode);
            // Read the response
			if (response.substring(0,2).equals("OK")) {
				// Update lastSync
			    deck.setLastSync(Double.parseDouble(response.substring(3, response.length()-3)));
			    deck.commitToDB();
			    // Make sure we don't set modified later than lastSync when we do closeDeck later:
			    deck.setLastLoaded(deck.getModified());
			    // Remove temp file
			    tmpFile.delete();
			}
            log.info("Finished!");
            result.put("code", String.valueOf(responseCode));
            result.put("message", response);
        } catch (ClientProtocolException e) {
            log.error("ClientProtocolException", e);
            result.put("code", "ClientProtocolException");
            exc = e;
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException", e);
            result.put("code", "UnsupportedEncodingException");
            exc = e;
        } catch (MalformedURLException e) {
            log.error("MalformedURLException", e);
            result.put("code", "MalformedURLException");
            exc = e;
        } catch (IOException e) {
            log.error("IOException", e);
            result.put("code", "IOException");
            exc = e;
        }

        if (exc != null) {
            // Sometimes the exception has null message and we have to get it from its cause
            while (exc.getMessage() == null && exc.getCause() != null) {
                exc = exc.getCause();
            }
            result.put("message", exc.getMessage());
        }
        return result;
    }


    public static HashMap<String, String> fullSyncFromServer(String password, String username, String deckName, String deckPath) {
        HashMap<String, String> result = new HashMap<String, String>();
        Throwable exc = null;
        try {
            String data = "p=" + URLEncoder.encode(password, "UTF-8") + "&u=" + URLEncoder.encode(username, "UTF-8")
                    + "&d=" + URLEncoder.encode(deckName, "UTF-8");

            // log.info("Data json = " + data);
            HttpPost httpPost = new HttpPost(AnkiDroidProxy.SYNC_URL + "fulldown");
            StringEntity entity = new StringEntity(data);
            httpPost.setEntity(entity);
            httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");
            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entityResponse = response.getEntity();
            InputStream content = entityResponse.getContent();
            int responseCode = response.getStatusLine().getStatusCode();
            String tempDeckPath = deckPath + ".tmp";
            if (responseCode == 200) {
                Utils.writeToFile(new InflaterInputStream(content), tempDeckPath);
                File newFile = new File(tempDeckPath);
                //File oldFile = new File(deckPath);
                if (newFile.renameTo(new File(deckPath))) {
                    result.put("code", "200");
                } else {
                    result.put("code", "PermissionError");
                    result.put("message", "Can't overwrite old deck with downloaded from server");
                }
            } else {
                result.put("code", String.valueOf(responseCode));
                result.put("message", Utils.convertStreamToString(content));
            }
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException", e);
            result.put("code", "UnsupportedEncodingException");
            exc = e;
        } catch (ClientProtocolException e) {
        	log.error("ClientProtocolException", e);
            result.put("code", "ClientProtocolException");
            exc = e;
        } catch (IOException e) {
        	log.error("IOException", e);
            result.put("code", "IOException");
            exc = e;
        }

        if (exc != null) {
            // Sometimes the exception has null message and we have to get it from its cause
            while (exc.getMessage() == null && exc.getCause() != null) {
                exc = exc.getCause();
            }
            result.put("message", exc.getMessage());
        }
        return result;
    }

}
