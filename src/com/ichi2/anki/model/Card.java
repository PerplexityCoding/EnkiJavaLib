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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ichi2.anki.Utils;

/**
 * A card is a presentation of a fact, and has two sides: a question and an answer. Any number of fields can appear on
 * each side. When you add a fact to Anki, cards which show that fact are generated. Some models generate one card,
 * others generate more than one.
 *
 * @see http://ichi2.net/anki/wiki/KeyTermsAndConcepts#Cards
 */
public class Card {

    // TODO: Javadoc.
	public static Logger log = LoggerFactory.getLogger(Image.class);

    /** Card types. */
    public static final int TYPE_FAILED = 0;
    public static final int TYPE_REV = 1;
    public static final int TYPE_NEW = 2;

    /** Card states. */
    public static final String STATE_NEW = "new";
    public static final String STATE_YOUNG = "young";
    public static final String STATE_MATURE = "mature";

    /** Auto priorities. */
    public static final int PRIORITY_NONE = 0;
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_NORMAL = 2;
    public static final int PRIORITY_MEDIUM = 3;
    public static final int PRIORITY_HIGH = 4;

    /** Manual priorities. */
    public static final int PRIORITY_REVIEW_EARLY = -1;
    public static final int PRIORITY_BURIED = -2;
    public static final int PRIORITY_SUSPENDED = -3;

    /** Ease. */
    public static final int EASE_NONE = 0;
    public static final int EASE_FAILED = 1;
    public static final int EASE_HARD = 2;
    public static final int EASE_MID = 3;
    public static final int EASE_EASY = 4;

    /** Tags src constants. */
    public static final int TAGS_FACT = 0;
    public static final int TAGS_MODEL = 1;
    public static final int TAGS_TEMPL = 2;

    private static final int LEARNT_THRESHOLD = 7;
    public static final int MATURE_THRESHOLD = 21;

    private static final double MAX_TIMER = 60.0;

    // BEGIN SQL table entries
    private long mId; // Primary key
    private long mFactId; // Foreign key facts.id
    private long mCardModelId; // Foreign key cardModels.id
    private double mCreated = Utils.now();
    private double mModified = Utils.now();
    private String mTags = "";
    private int mOrdinal;
    // Cached - changed on fact update
    private String mQuestion = "";
    private String mAnswer = "";
    private int mPriority = PRIORITY_NORMAL;
    private double mInterval = 0;
    private double mLastInterval = 0;
    private double mDue = Utils.now();
    private double mLastDue = 0;
    private double mFactor = Deck.INITIAL_FACTOR;
    private double mLastFactor = Deck.INITIAL_FACTOR;
    private double mFirstAnswered = 0;
    // Stats
    private int mReps = 0;
    private int mSuccessive = 0;
    private double mAverageTime = 0;
    private double mReviewTime = 0;
    private int mYoungEase0 = 0;
    private int mYoungEase1 = 0;
    private int mYoungEase2 = 0;
    private int mYoungEase3 = 0;
    private int mYoungEase4 = 0;
    private int mMatureEase0 = 0;
    private int mMatureEase1 = 0;
    private int mMatureEase2 = 0;
    private int mMatureEase3 = 0;
    private int mMatureEase4 = 0;
    // This duplicates the above data, because there's no way to map imported
    // data to the above
    private int mYesCount = 0;
    private int mNoCount = 0;
    private double mSpaceUntil = 0;      // obsolete in libanki 1.1.4
    // relativeDelay is reused as type without scheduling (ie, it remains 0-2 even if card is suspended, etc)
    private double mRelativeDelay = 0;
    private int mIsDue = 0;              // obsolete in libanki 1.1
    private int mType = TYPE_NEW;
    private double mCombinedDue = 0;
    // END SQL table entries

    public Deck mDeck;

    // BEGIN JOINed variables
    private CardModel mCardModel;
    private Fact mFact;
    private String[] mTagsBySrc;
    // END JOINed variables

    private double mTimerStarted;
    private double mTimerStopped;
    private double mFuzz = 0;

    // Leech flags, not read from database, only set to true during the actual suspension
    private boolean isLeechMarked;
    private boolean isLeechSuspended;

    public Card(Deck deck, Fact fact, CardModel cardModel, double created) {
        mTags = "";
        mTagsBySrc = new String[TAGS_TEMPL + 1];
        mTagsBySrc[TAGS_FACT] = "";
        mTagsBySrc[TAGS_MODEL] = "";
        mTagsBySrc[TAGS_TEMPL] = "";

        mId = Utils.genID();
        // New cards start as new & due
        mType = TYPE_NEW;
        mRelativeDelay = mType;
        mTimerStarted = Double.NaN;
        mTimerStopped = Double.NaN;
        mModified = Utils.now();
        if (Double.isNaN(created)) {
            mCreated = created;
            mDue = created;
        } else {
            mDue = mModified;
        }
        isLeechSuspended = false;
        mCombinedDue = mDue;
        mDeck = deck;
        mFact = fact;
        if (fact != null) {
            mFactId = fact.getId();
        }
        mCardModel = cardModel;
        if (cardModel != null) {
            mCardModelId = cardModel.getId();
            mOrdinal = cardModel.getOrdinal();
        }
    }

	/**
	 * Format qa
	 */
	public void rebuildQA(Deck deck) {
		rebuildQA(deck, true);
	}
	
	public void rebuildQA(Deck deck, boolean media) {
        // Format qa
		if (mFact != null && mCardModel != null) {
			HashMap<String, String> qa = CardModel.formatQA(mFact, mCardModel, splitTags());

            if (media) {
                // Find old media references
                HashMap<String, Integer> files = new HashMap<String, Integer>();
                ArrayList<String> filesFromQA = Media.mediaFiles(mQuestion);
                filesFromQA.addAll(Media.mediaFiles(mAnswer));
                for (String f : filesFromQA) {
                    if (files.containsKey(f)) {
                        files.put(f, files.get(f) - 1);
                    } else {
                        files.put(f, -1);
                    }
                }
                // Update q/a
                mQuestion = qa.get("question");
                mAnswer = qa.get("answer");
                // Determine media delta
                filesFromQA = Media.mediaFiles(mQuestion);
                filesFromQA.addAll(Media.mediaFiles(mAnswer));
                for (String f : filesFromQA) {
                    if (files.containsKey(f)) {
                        files.put(f, files.get(f) + 1);
                    } else {
                        files.put(f, 1);
                    }
                }
                // Update media counts if we're attached to deck
                for (Entry<String, Integer> entry : files.entrySet()) {
                    Media.updateMediaCount(deck, entry.getKey(), entry.getValue());
                }
            } else {
                // Update q/a
                mQuestion = qa.get("question");
                mAnswer = qa.get("answer");
            }
            setModified();
		}
	}

    public Card(Deck deck) {
        this(deck, null, null, Double.NaN);
    }


    public Fact getFact() {
        if (mFact == null) {
            mFact = new Fact(mDeck, mFactId);
        }
        return mFact;
    }


    public void setModified() {
        mModified = Utils.now();
    }


    public void setTimerStart(double time) {
        mTimerStarted = time;
    }


    public void startTimer() {
        mTimerStarted = Utils.now();
    }


    public void stopTimer() {
        mTimerStopped = Utils.now();
    }


    public void resumeTimer() {
        if (!Double.isNaN(mTimerStarted) && !Double.isNaN(mTimerStopped)) {
            mTimerStarted += Utils.now() - mTimerStopped;
            mTimerStopped = Double.NaN;
        } else {
        	log.info("Card Timer: nothing to resume");
        }
    }


    public double thinkingTime() {
        if (Double.isNaN(mTimerStopped)) {
            return (Utils.now() - mTimerStarted);
        } else {
            return (mTimerStopped - mTimerStarted);
        }
    }


    public double totalTime() {
        return (Utils.now() - mTimerStarted);
    }

    public double getFuzz() {
    	if (mFuzz == 0) {
    		genFuzz();
    	}
    	return mFuzz;
    }

    public void genFuzz() {
        // Random rand = new Random();
        // mFuzz = 0.95 + (0.1 * rand.nextDouble());
        mFuzz = (double) Math.random();
    }


    // XXX Unused
//    public String htmlQuestion(String type, boolean align) {
//        return null;
//    }
//
//
//    public String htmlAnswer(boolean align) {
//        return htmlQuestion("answer", align);
//    }


    public void updateStats(int ease, String state) {
        char[] newState = state.toCharArray();
        mReps += 1;
        if (ease > EASE_FAILED) {
            mSuccessive += 1;
        } else {
            mSuccessive = 0;
        }

        double delay = Math.min(totalTime(), MAX_TIMER);
        // Ignore any times over 60 seconds
        mReviewTime += delay;
        if (mAverageTime != 0) {
            mAverageTime = (mAverageTime + delay) / 2.0;
        } else {
            mAverageTime = delay;
        }
        // We don't track first answer for cards
        if (STATE_NEW.equalsIgnoreCase(state)) {
            newState = STATE_YOUNG.toCharArray();
        }

        // Update ease and yes/no count
        // We want attr to be of the form mYoungEase3
        newState[0] = Character.toUpperCase(newState[0]);
        String attr = "m" + String.valueOf(newState) + String.format("Ease%d", ease);
        try {
            java.lang.reflect.Field f = this.getClass().getDeclaredField(attr);
            f.setInt(this, f.getInt(this) + 1);
        } catch (Exception e) {
            log.error("Failed to update " + attr + " : " + e.getMessage());
        }

        if (ease < EASE_HARD) {
            mNoCount += 1;
        } else {
            mYesCount += 1;
        }
        if (mFirstAnswered == 0) {
            mFirstAnswered = Utils.now();
        }
        setModified();
    }


    public void updateFactor(int ease, double averageFactor) {
        mLastFactor = mFactor;
        if (isNew()) {
            mFactor = averageFactor; // card is new, inherit beginning factor
        }
        if (isRev() && !isBeingLearnt()) {
            if (ease == EASE_FAILED) {
                mFactor -= 0.20;
            } else if (ease == EASE_HARD) {
                mFactor -= 0.15;
            }
        }
        if (ease == EASE_EASY) {
            mFactor += 0.10;
        }
        mFactor = Math.max(Deck.FACTOR_FOUR, mFactor);
    }


    public double adjustedDelay(int ease) {
        double now = Utils.now();
        if (isNew()) {
            return 0;
        }
        if (mCombinedDue <= now) {
            return (now -mDue) / 86400.0;
        } else {
            return (now - mCombinedDue) / 86400.0;
        }
    }


    /**
     * Suspend this card.
     */
    public void suspend() {
        long[] ids = new long[1];
        ids[0] = mId;
        mDeck.suspendCards(ids);
        mDeck.reset();
    }


    /**
     * Unsuspend this card.
     */
    public void unsuspend() {
        long[] ids = new long[1];
        ids[0] = mId;
        mDeck.unsuspendCards(ids);
    }


    public boolean getSuspendedState() {
        return mDeck.getSuspendedState(mId);
    }

    /**
     * Delete this card.
     */
    public void delete() {
        List<String> ids = new ArrayList<String>();
        ids.add(Long.toString(mId));
        mDeck.deleteCards(ids);
    }


    public String getState() {
        if (isNew()) {
            return STATE_NEW;
        } else if (mInterval > MATURE_THRESHOLD) {
            return STATE_MATURE;
        }
        return STATE_YOUNG;
    }


    /**
     * Check if a card is a new card.
     * @return True if a card has never been seen before.
     */
    public boolean isNew() {
        return mReps == 0;
    }


    /**
     * Check if this is a revision of a successfully answered card.
     * @return True if the card was successfully answered last time.
     */
    public boolean isRev() {
        return mSuccessive != 0;
    }


    /**
     * Check if a card is being learnt.
     * @return True if card should use present intervals.
     */
    public boolean isBeingLearnt() {
        return mLastInterval < LEARNT_THRESHOLD;
    }


    public String[] splitTags() {
        String[] tags = new String[]{
            getFact().getTags(),
            Model.getModel(mDeck, getFact().getModelId(), true).getTags(),
            getCardModel().getName()
        };
        return tags;
    }


    private String allTags() {
        // Non-Canonified string of fact and model tags
        if ((mTagsBySrc[TAGS_FACT].length() > 0) && (mTagsBySrc[TAGS_MODEL].length() > 0)) {
            return mTagsBySrc[TAGS_FACT] + "," + mTagsBySrc[TAGS_MODEL];
        } else if (mTagsBySrc[TAGS_FACT].length() > 0) {
            return mTagsBySrc[TAGS_FACT];
        } else {
            return mTagsBySrc[TAGS_MODEL];
        }
    }


    public boolean hasTag(String tag) {
        return (allTags().indexOf(tag) != -1);
    }


    public boolean isMarked() throws SQLException {
    	int markedId = mDeck.getMarketTagId();
    	if (markedId == -1) {
    		return false;
    	} else {
    		return (mDeck.getDB().queryScalar("SELECT count(*) FROM cardTags WHERE cardId = " + mId + " AND tagId = " + markedId + " LIMIT 1") != 0);
    	}
    }

    // FIXME: Should be removed. Calling code should directly interact with Model
    public CardModel getCardModel() {
        Model myModel = Model.getModel(mDeck, mCardModelId, false);
        return myModel.getCardModel(mCardModelId);
    }


    // Loading tags for this card. Needed when:
    // - we modify the card fields and need to update question and answer.
    // - we check is a card is marked
    public void loadTags() {
    	ResultSet result = null;

        int tagSrc = 0;

        // Flush tags
        for (int i = 0; i < mTagsBySrc.length; i++) {
            mTagsBySrc[i] = "";
        }

        try {
        	result = mDeck.getDB().rawQuery(
                    "SELECT tags.tag, cardTags.src "
                    + "FROM cardTags JOIN tags ON cardTags.tagId = tags.id " + "WHERE cardTags.cardId = " + mId
                    + " AND cardTags.src in (" + TAGS_FACT + ", " + TAGS_MODEL + "," + TAGS_TEMPL + ") "
                    + "ORDER BY cardTags.id");
        	while (result.next()) {
        		tagSrc = result.getInt(1);
	            if (mTagsBySrc[tagSrc].length() > 0) {
	                mTagsBySrc[tagSrc] += "," + result.getString(1);
	            } else {
	                mTagsBySrc[tagSrc] += result.getString(1);
	            }
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


    public void resetCard() {
        log.info("Reset Card: " + mId);
    	mModified = Utils.now();
    	mPriority = PRIORITY_NORMAL;
        mInterval = 0;
        mLastInterval = 0;
        mDue = Utils.now();
        mLastDue = 0;
        mFactor = Deck.INITIAL_FACTOR;
        mLastFactor = Deck.INITIAL_FACTOR;
        mFirstAnswered = 0;
        mReps = 0;
        mSuccessive = 0;
        mAverageTime = 0;
        mReviewTime = 0;
        mYoungEase0 = 0;
        mYoungEase1 = 0;
        mYoungEase2 = 0;
        mYoungEase3 = 0;
        mYoungEase4 = 0;
        mMatureEase0 = 0;
        mMatureEase1 = 0;
        mMatureEase2 = 0;
        mMatureEase3 = 0;
        mMatureEase4 = 0;
        mYesCount = 0;
        mNoCount = 0;
        mRelativeDelay = 0;
        mType = TYPE_NEW;
        mCombinedDue = 0;
        toDB();
    }


    public boolean fromDB(long id) {
        ResultSet result = null;

        try {
        	result = mDeck.getDB().rawQuery(
                    "SELECT id, factId, cardModelId, created, modified, tags, "
                            + "ordinal, question, answer, priority, interval, lastInterval, "
                            + "due, lastDue, factor, lastFactor, firstAnswered, reps, "
                            + "successive, averageTime, reviewTime, youngEase0, youngEase1, "
                            + "youngEase2, youngEase3, youngEase4, matureEase0, matureEase1, "
                            + "matureEase2, matureEase3, matureEase4, yesCount, noCount, "
                            + "spaceUntil, isDue, type, combinedDue, relativeDelay " + "FROM cards " + "WHERE id = " + id);
            if (!result.next()) {
                log.warn("Card.java (fromDB(id)): No result from query.");
                return false;
            }

            int i = 1;
            mId = result.getLong(i++);
            mFactId = result.getLong(i++);
            mCardModelId = result.getLong(i++);
            mCreated = result.getDouble(i++);
            mModified = result.getDouble(i++);
            mTags = result.getString(i++);
            mOrdinal = result.getInt(i++);
            mQuestion = result.getString(i++);
            mAnswer = result.getString(i++);
            mPriority = result.getInt(i++);
            mInterval = result.getDouble(i++);
            mLastInterval = result.getDouble(i++);
            mDue = result.getDouble(i++);
            mLastDue = result.getDouble(i++);
            mFactor = result.getDouble(i++);
            mLastFactor = result.getDouble(i++);
            mFirstAnswered = result.getDouble(i++);
            mReps = result.getInt(i++);
            mSuccessive = result.getInt(i++);
            mAverageTime = result.getDouble(i++);
            mReviewTime = result.getDouble(i++);
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
            mYesCount = result.getInt(i++);
            mNoCount = result.getInt(i++);
            mSpaceUntil = result.getDouble(i++);
            mIsDue = result.getInt(i++);
            mType = result.getInt(i++);
            mCombinedDue = result.getDouble(i++);
            mRelativeDelay = result.getDouble(i++);
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

        // TODO: Should also read JOINed entries CardModel and Fact.

        return true;
    }

    // TODO: Remove Redundancies
    // I did a separated method because I don't want to interfere with other code while fact adding is not tested.
    public void addToDb(){
        if (isNew()) {
            mType = TYPE_NEW;
        } else if (isRev()) {
            mType = TYPE_REV;
        } else {
            mType = TYPE_FAILED;
        }

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("id", mId);
        values.put("factId", mFactId);
        values.put("cardModelId", mCardModelId);
        values.put("created", mCreated);
        values.put("modified", mModified);
        values.put("tags", mTags);
        values.put("ordinal", mOrdinal);
        values.put("question", mQuestion);
        values.put("answer", mAnswer);
        values.put("priority", mPriority);
        values.put("interval", mInterval);
        values.put("lastInterval", mLastInterval);
        values.put("due", mDue);
        values.put("lastDue", mLastDue);
        values.put("factor", mFactor);
        values.put("lastFactor", mLastFactor);
        values.put("firstAnswered", mFirstAnswered);
        values.put("reps", mReps);
        values.put("successive", mSuccessive);
        values.put("averageTime", mAverageTime);
        values.put("reviewTime", mReviewTime);
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
        values.put("yesCount", mYesCount);
        values.put("noCount", mNoCount);
        values.put("spaceUntil", mSpaceUntil);
        values.put("isDue", mIsDue);
        values.put("type", mType);
        values.put("combinedDue", Math.max(mSpaceUntil, mDue));
        values.put("relativeDelay", 0.0);
        mDeck.getDB().insert(mDeck, "cards", null, values);

    }

    public void toDB() {

    	Map<String, Object> values = new HashMap<String, Object>();
        values.put("factId", mFactId);
        values.put("cardModelId", mCardModelId);
        values.put("created", mCreated);
        values.put("modified", mModified);
        values.put("tags", mTags);
        values.put("ordinal", mOrdinal);
        values.put("question", mQuestion);
        values.put("answer", mAnswer);
        values.put("priority", mPriority);
        values.put("interval", mInterval);
        values.put("lastInterval", mLastInterval);
        values.put("due", mDue);
        values.put("lastDue", mLastDue);
        values.put("factor", mFactor);
        values.put("lastFactor", mLastFactor);
        values.put("firstAnswered", mFirstAnswered);
        values.put("reps", mReps);
        values.put("successive", mSuccessive);
        values.put("averageTime", mAverageTime);
        values.put("reviewTime", mReviewTime);
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
        values.put("yesCount", mYesCount);
        values.put("noCount", mNoCount);
        values.put("spaceUntil", mSpaceUntil);
        values.put("isDue", 0);
        values.put("type", mType);
        values.put("combinedDue", mCombinedDue);
        values.put("relativeDelay", mRelativeDelay);
        mDeck.getDB().update(mDeck, "cards", values, "id = " + mId, true);

        // TODO: Should also write JOINED entries: CardModel and Fact.
    }


    /**
     * Commit question and answer fields to database.
     */
    public void updateQAfields() {
        setModified();
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("modified", mModified);
        values.put("question", mQuestion);
        values.put("answer", mAnswer);
        mDeck.getDB().update(mDeck, "cards", values, "id = " + mId);
    }


    public Map<String, Object> getAnswerValues() {
    	Map<String, Object> values = new HashMap<String, Object>();
        values.put("modified", mModified);
        values.put("priority", mPriority);
        values.put("interval", mInterval);
        values.put("lastInterval", mLastInterval);
        values.put("due", mDue);
        values.put("lastDue", mLastDue);
        values.put("factor", mFactor);
        values.put("lastFactor", mLastFactor);
        values.put("firstAnswered", mFirstAnswered);
        values.put("reps", mReps);
        values.put("successive", mSuccessive);
        values.put("averageTime", mAverageTime);
        values.put("reviewTime", mReviewTime);
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
        values.put("yesCount", mYesCount);
        values.put("noCount", mNoCount);
        values.put("type", mType);
        values.put("combinedDue", mCombinedDue);
        values.put("relativeDelay", mRelativeDelay);
        return values;
    }


    public long getId() {
        return mId;
    }

    public void setLastInterval(double lastInterval) {
        mLastInterval = lastInterval;
    }


    public double getLastInterval() {
        return mLastInterval;
    }


    public void setInterval(double interval) {
        mInterval = interval;
    }


    public double getInterval() {
        return mInterval;
    }


    public void setLastFactor(double lastFactor) {
        mLastFactor = lastFactor;
    }


    public double getLastFactor() {
        return mLastFactor;
    }


    public double getFactor() {
        return mFactor;
    }


    public int getReps() {
        return mReps;
    }


    public int getYesCount() {
        return mYesCount;
    }


    public int getNoCount() {
        return mNoCount;
    }


    public void setQuestion(String question) {
        mQuestion = question;
    }


    public String getQuestion() {
        return mQuestion;
    }


    public void setAnswer(String answer) {
        mAnswer = answer;
    }


    public String getAnswer() {
        return mAnswer;
    }


    public void setModified(double modified) {
        mModified = modified;
    }


    public void setCombinedDue(double combinedDue) {
        mCombinedDue = combinedDue;
    }


    public double getCombinedDue() {
        return mCombinedDue;
    }


    public void setLastDue(double lastDue) {
        mLastDue = lastDue;
    }


    public void setDue(double due) {
        mDue = due;
    }


    public double getDue() {
        return mDue;
    }


    public void setIsDue(int isDue) {
        mIsDue = isDue;
    }


    /**
     * Check whether the card is due.
     * @return True if the card is due, false otherwise
     */
    public boolean isDue() {
        return (mIsDue == 1);
    }


    public long getFactId() {
        return mFactId;
    }


    public void setSpaceUntil(double spaceUntil) {
        mSpaceUntil = spaceUntil;
    }


    public void setRelativeDelay(double relativeDelay) {
        mRelativeDelay = relativeDelay;
    }


    public void setPriority(int priority) {
        mPriority = priority;
    }


    public int getPriority() {
        return mPriority;
    }


    public int getType() {
        return mType;
    }


    public void setType(int type) {
        mType = type;
    }


    public long getCardModelId() {
        return mCardModelId;
    }


    public double nextInterval(Card card, int ease) {
        return mDeck.nextInterval(card, ease);
    }

    // Leech flag
    public boolean getLeechFlag() {
        return isLeechMarked;
    }
    public void setLeechFlag(boolean flag) {
        isLeechMarked = flag;
    }
    // Suspended flag
    public boolean getSuspendedFlag() {
        return isLeechSuspended;
    }
    public void setSuspendedFlag(boolean flag) {
        isLeechSuspended = flag;
    }

    public int getSuccessive() {
        return mSuccessive;
    }
    
    /**
     * The cardModel defines a field typeAnswer. If it is empty, then no answer should be typed.
     * Otherwise a typed answer should be compared to the value of field related to a cards fact.
     * A field is found based on the factId in the card and the fieldModelId.
     * The fieldModel's id is found by searching with the typeAnswer name and cardModel's modelId
     * 
     * @return 2 dimensional array with answer value at index=0 and fieldModel's class at index=1
     * null if typeAnswer is empty (i.e. do not prompt for answer). Otherwise a string (which can be empty) from the actual field value.
     * The fieldModel's id is correctly hexafied and formatted for class attribute of span for formatting 
     */
    public String[] getComparedFieldAnswer() {
    	String[] returnArray = new String[2];
    	CardModel myCardModel = this.getCardModel();
    	String typeAnswer = myCardModel.getTypeAnswer();
        // Check if we have a valid field to use as the answer to type.
    	if (null == typeAnswer || 0 == typeAnswer.trim().length()) {
		// no field specified, compare with whole answer
    		returnArray[0] = mAnswer;
    		returnArray[1] = "";
                return returnArray;
    	}

        Model myModel = Model.getModel(mDeck, myCardModel.getModelId(), true);
    	TreeMap<Long, FieldModel> fieldModels = myModel.getFieldModels();
    	FieldModel myFieldModel = null;
    	long myFieldModelId = 0l;
    	for (Entry<Long, FieldModel> entry : fieldModels.entrySet()) {
    		myFieldModel = entry.getValue();
    		myFieldModelId = myFieldModel.match(myCardModel.getModelId(), typeAnswer);
    		if (myFieldModelId != 0l) {
    			break;
    		}
    	}

        // Just in case we do not find the matching field model.
        if (myFieldModelId == 0) {
            log.error("could not find field model for type answer: " + typeAnswer);
            returnArray[0] = null;
            return null;
        }

    	returnArray[0] = Field.fieldValuefromDb(this.mDeck, this.mFactId, myFieldModelId);
    	returnArray[1] = "fm" + Long.toHexString(myFieldModelId);
    	return returnArray;
    }
}
