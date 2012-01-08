/****************************************************************************************
 * Copyright (c) 2009 Daniel Svärd <daniel.svard@gmail.com>                             *
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

package com.ichi2.anki.model;

import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ichi2.anki.Utils;

/**
 * Deck statistics.
 */
public class Stats {
	
	private static Logger log = LoggerFactory.getLogger(Stats.class);

    public static final int STATS_LIFE = 0;
    public static final int STATS_DAY = 1;
    
    // BEGIN: SQL table columns
    private long mId;
    private int mType;
    private Date mDay;
    private int mReps;
    private double mAverageTime;
    private double mReviewTime;
    // Next two columns no longer used
    private double mDistractedTime;
    private int mDistractedReps;
    private int mNewEase0;
    private int mNewEase1;
    private int mNewEase2;
    private int mNewEase3;
    private int mNewEase4;
    private int mYoungEase0;
    private int mYoungEase1;
    private int mYoungEase2;
    private int mYoungEase3;
    private int mYoungEase4;
    private int mMatureEase0;
    private int mMatureEase1;
    private int mMatureEase2;
    private int mMatureEase3;
    private int mMatureEase4;
    // END: SQL table columns

    private Deck mDeck;


    public Stats(Deck deck) {
        mDeck = deck;
        mDay = null;
        mReps = 0;
        mAverageTime = 0;
        mReviewTime = 0;
        mDistractedTime = 0;
        mDistractedReps = 0;
        mNewEase0 = 0;
        mNewEase1 = 0;
        mNewEase2 = 0;
        mNewEase3 = 0;
        mNewEase4 = 0;
        mYoungEase0 = 0;
        mYoungEase1 = 0;
        mYoungEase2 = 0;
        mYoungEase3 = 0;
        mMatureEase0 = 0;
        mMatureEase1 = 0;
        mMatureEase2 = 0;
        mMatureEase3 = 0;
        mMatureEase4 = 0;
    }


    public void fromDB(long id) {
        ResultSet result = null;

        try {
            log.info("Reading stats from DB...");
            result = mDeck.getDB().rawQuery(
                    "SELECT * " + "FROM stats WHERE id = " + String.valueOf(id));

            if (!result.next()) {
                return;
            }

            int i = 1;
            mId = result.getLong(i++);
            mType = result.getInt(i++);
            mDay = Date.valueOf(result.getString(i++));
            mReps = result.getInt(i++);
            mAverageTime = result.getDouble(i++);
            mReviewTime = result.getDouble(i++);
            mDistractedTime = result.getDouble(i++);
            mDistractedReps = result.getInt(i++);
            mNewEase0 = result.getInt(i++);
            mNewEase1 = result.getInt(i++);
            mNewEase2 = result.getInt(i++);
            mNewEase3 = result.getInt(i++);
            mNewEase4 = result.getInt(i++);
            mYoungEase0 = result.getInt(i++);
            mYoungEase1 = result.getInt(i++);
            mYoungEase2 = result.getInt(i++);
            mYoungEase3 = result.getInt(i++);
            mYoungEase4 = result.getInt(i++);
            mMatureEase0 = result.getInt(i++);
            mMatureEase1 = result.getInt(i++);
            mMatureEase2 = result.getInt(i++);
            mMatureEase3 = result.getInt(i++);
            mMatureEase4 = result.getInt(i++);
            
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


    public void create(int type, Date day) {
        log.info("Creating new stats for " + day.toString() + "...");
        mType = type;
        mDay = day;

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("type", type);
        values.put("day", day.toString());
        values.put("reps", 0);
        values.put("averageTime", 0);
        values.put("reviewTime", 0);
        values.put("distractedTime", 0);
        values.put("distractedReps", 0);
        values.put("newEase0", 0);
        values.put("newEase1", 0);
        values.put("newEase2", 0);
        values.put("newEase3", 0);
        values.put("newEase4", 0);
        values.put("youngEase0", 0);
        values.put("youngEase1", 0);
        values.put("youngEase2", 0);
        values.put("youngEase3", 0);
        values.put("youngEase4", 0);
        values.put("matureEase0", 0);
        values.put("matureEase1", 0);
        values.put("matureEase2", 0);
        values.put("matureEase3", 0);
        values.put("matureEase4", 0);
        mId = mDeck.getDB().insert(mDeck, "stats", null, values);
    }


    public void toDB() {
        mDeck.getDB().update(mDeck, "stats", getValues(), "id = " + mId);
    }


    public void toDB(Map<String, Object> oldValues) {
        mDeck.getDB().update(mDeck, "stats", getValues(), "id = " + mId, true,
        	(HashMap<String, Object>[]) new Object[] {oldValues}, new String[] {"id = " + mId});
    }


    private Map<String, Object> getValues() {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("type", mType);
        values.put("day", mDay.toString());
        values.put("reps", mReps);
        values.put("averageTime", mAverageTime);
        values.put("reviewTime", mReviewTime);
        values.put("newEase0", mNewEase0);
        values.put("newEase1", mNewEase1);
        values.put("newEase2", mNewEase2);
        values.put("newEase3", mNewEase3);
        values.put("newEase4", mNewEase4);
        values.put("youngEase0", mYoungEase0);
        values.put("youngEase1", mYoungEase1);
        values.put("youngEase2", mYoungEase2);
        values.put("youngEase3", mYoungEase3);
        values.put("youngEase4", mYoungEase4);
        values.put("matureEase0", mMatureEase0);
        values.put("matureEase1", mMatureEase1);
        values.put("matureEase2", mMatureEase2);
        values.put("matureEase3", mMatureEase3);
        values.put("matureEase4", mMatureEase4);
	return values;
    }


    public static void updateAllStats(Stats global, Stats daily, Card card, int ease, String oldState) {
        updateStats(global, card, ease, oldState);
        updateStats(daily, card, ease, oldState);
    }


    public static void updateStats(Stats stats, Card card, int ease, String oldState) {
    	Map<String, Object> oldValues = new HashMap<String, Object>();
        char[] newState = oldState.toCharArray();
        stats.mReps += 1;
        double delay = card.totalTime();
        if (delay >= 60) {
            stats.mReviewTime += 60;
        } else {
            stats.mReviewTime += delay;
            stats.mAverageTime = (stats.mReviewTime / stats.mReps);
        }
        // update eases
        // We want attr to be of the form mYoungEase3
        newState[0] = Character.toUpperCase(newState[0]);
        StringBuilder attr = new StringBuilder();
		attr.append("m").append(String.valueOf(newState)).append(String.format("Ease%d", ease));
        try {
            Field f = stats.getClass().getDeclaredField(attr.toString());
            f.setInt(stats, f.getInt(stats) + 1);
        } catch (Exception e) {
            log.error("Failed to update " + attr.toString() + " : " + e.getMessage());
        }
        stats.toDB(oldValues);
    }


    public JSONObject bundleJson() {
        JSONObject bundledStat = new JSONObject();

        try {
            bundledStat.put("type", mType);
            bundledStat.put("day", Utils.dateToOrdinal(mDay));
            bundledStat.put("reps", mReps);
            bundledStat.put("averageTime", mAverageTime);
            bundledStat.put("reviewTime", mReviewTime);
            bundledStat.put("distractedTime", mDistractedTime);
            bundledStat.put("distractedReps", mDistractedReps);
            bundledStat.put("newEase0", mNewEase0);
            bundledStat.put("newEase1", mNewEase1);
            bundledStat.put("newEase2", mNewEase2);
            bundledStat.put("newEase3", mNewEase3);
            bundledStat.put("newEase4", mNewEase4);
            bundledStat.put("youngEase0", mYoungEase0);
            bundledStat.put("youngEase1", mYoungEase1);
            bundledStat.put("youngEase2", mYoungEase2);
            bundledStat.put("youngEase3", mYoungEase3);
            bundledStat.put("youngEase4", mYoungEase4);
            bundledStat.put("matureEase0", mMatureEase0);
            bundledStat.put("matureEase1", mMatureEase1);
            bundledStat.put("matureEase2", mMatureEase2);
            bundledStat.put("matureEase3", mMatureEase3);
            bundledStat.put("matureEase4", mMatureEase4);

        } catch (JSONException e) {
            log.info("JSONException = " + e.getMessage());
        }

        return bundledStat;
    }


    public void updateFromJson(JSONObject remoteStat) {
        try {
            mAverageTime = remoteStat.getDouble("averageTime");
            mDay = Utils.ordinalToDate(remoteStat.getInt("day"));
            mDistractedReps = remoteStat.getInt("distractedReps");
            mDistractedTime = remoteStat.getDouble("distractedTime");
            mMatureEase0 = remoteStat.getInt("matureEase0");
            mMatureEase1 = remoteStat.getInt("matureEase1");
            mMatureEase2 = remoteStat.getInt("matureEase2");
            mMatureEase3 = remoteStat.getInt("matureEase3");
            mMatureEase4 = remoteStat.getInt("matureEase4");
            mNewEase0 = remoteStat.getInt("newEase0");
            mNewEase1 = remoteStat.getInt("newEase1");
            mNewEase2 = remoteStat.getInt("newEase2");
            mNewEase3 = remoteStat.getInt("newEase3");
            mNewEase4 = remoteStat.getInt("newEase4");
            mReps = remoteStat.getInt("reps");
            mReviewTime = remoteStat.getDouble("reviewTime");
            mType = remoteStat.getInt("type");
            mYoungEase0 = remoteStat.getInt("youngEase0");
            mYoungEase1 = remoteStat.getInt("youngEase1");
            mYoungEase2 = remoteStat.getInt("youngEase2");
            mYoungEase3 = remoteStat.getInt("youngEase3");
            mYoungEase4 = remoteStat.getInt("youngEase4");

            toDB();
        } catch (JSONException e) {
            log.info("JSONException = " + e.getMessage());
        }
    }


    public static Stats globalStats(Deck deck) {
        log.info("Getting global stats...");
        int type = STATS_LIFE;
        Date today = Utils.genToday(deck.getUtcOffset());
        ResultSet result = null;
        Stats stats = null;

        try {
        	result = deck.getDB().rawQuery(
                    "SELECT id " + "FROM stats WHERE type = " + String.valueOf(type));

            if (result.next()) {
                stats = new Stats(deck);
                stats.fromDB(result.getLong(1));
                return stats;
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
        stats = new Stats(deck);
        stats.create(type, today);
        stats.mType = type;
        return stats;
    }


    public static Stats dailyStats(Deck deck) {
        log.info("Getting daily stats...");
        int type = STATS_DAY;
        Date today = Utils.genToday(deck.getUtcOffset());
        Stats stats = null;
        ResultSet result = null;

        try {
        	log.info("Trying to get stats for " + today.toString());
        	result = deck.getDB().rawQuery(
                    "SELECT id " + "FROM stats "
                    + "WHERE type = " + String.valueOf(type) + " and day = \"" + today.toString() + "\"");

            if (result.next()) {
                stats = new Stats(deck);
                stats.fromDB(result.getLong(1));
                return stats;
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
        stats = new Stats(deck);
        stats.create(type, today);
        stats.mType = type;
        return stats;
    }

    
    /**
     * @return the reps
     */
    public int getReps() {
        return mReps;
    }


    /**
     * @return the reps
     */
    public int getYesReps() {
        return mReps - mNewEase0 - mNewEase1 - mMatureEase0 - mMatureEase1 - mYoungEase0 - mYoungEase1;
    }


    /**
     * @return the average time
     */
    public double getAverageTime() {
        return mAverageTime;
    }


    /**
     * @return the day
     */
    public Date getDay() {
        return mDay;
    }


    /**
     * @return the share of no answers on young cards
     */
    public double getYesShare() {
    	if (mReps != 0) {
        	return 1 - (((double)(mNewEase0 + mNewEase1 + mYoungEase0 + mYoungEase1 + mMatureEase0 + mMatureEase1)) / (double)mReps);
    	} else {
        	return 0;
    	}
    }


    /**
     * @return the share of no answers on young cards
     */
    public double getMatureYesShare() {
    	double matureNo = mMatureEase0 + mMatureEase1;
    	double matureTotal = matureNo + mMatureEase2 + mMatureEase3 + mMatureEase4;
    	if (matureTotal != 0) {
        	return 1 - (matureNo / matureTotal);
    	} else {
        	return 0;
    	}
    }


    /**
     * @return the share of no answers on mature cards
     */
    public double getYoungNoShare() {
	double youngNo = mYoungEase0 + mYoungEase1;
	double youngTotal = youngNo + mYoungEase2 + mYoungEase3 + mYoungEase4;
    	if (youngTotal != 0) {
        	return youngNo / youngTotal;
    	} else {
        	return 0;
    	}
    }


    /**
     * @return the total number of cards marked as new
     */
    public int getNewCardsCount() {
        return mNewEase0 + mNewEase1 + mNewEase2 + mNewEase3 + mNewEase4;
    }
}
