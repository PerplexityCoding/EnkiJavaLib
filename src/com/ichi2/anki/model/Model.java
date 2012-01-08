/****************************************************************************************
 * Copyright (c) 2009 Daniel Svärd <daniel.svard@gmail.com>                             *
 * Copyright (c) 2010 Rick Gruber-Riemer <rick@vanosten.net>                            *
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ichi2.anki.Utils;
import com.ichi2.anki.db.AnkiDb;
import com.mindprod.common11.StringTools;

/**
 * Anki model. A model describes the type of information you want to input, and the type of cards which should be
 * generated. See http://ichi2.net/anki/wiki/KeyTermsAndConcepts#Models There can be several models in a Deck. A Model
 * is related to a Deck via attribute deckId. A CardModel is related to a Model via CardModel's modelId. A FieldModel is
 * related to a Model via FieldModel's modelId A Card has a link to CardModel via Card's cardModelId A Card has a link
 * to a Fact via Card's factId A Field has a link to a Fact via Field's factId A Field has a link to a FieldModel via
 * Field's fieldModelId => In order to get the CardModel and all FieldModels for a given Card: % the CardModel can
 * directly be retrieved from the DB using the Card's cardModelId % then from the retrieved CardModel we can get the
 * modelId % using the modelId we can get all FieldModels from the DB % (alternatively in the CardModel the qformat and
 * aformat fields could be parsed for relevant field names and then this used to only get the necessary fields. But this
 * adds a lot overhead vs. using a bit more memory)
 */
public class Model {

	public static Logger log = LoggerFactory.getLogger(Model.class);
	
    /** Singleton */
    // private static Model currentModel;

    /** Text align constants */
    private static final String[] align_text = { "center", "left", "right" };

    /**
     * A Map of the currently loaded Models. The Models are loaded from the database as soon as they are needed for the
     * first time. This is a compromise between RAM need, speed and the probability with which more than one Model is
     * needed. If only one model is needed, then RAM consumption is basically the same as having a static "currentModel"
     * variable. If more than one Model is needed, then more RAM is needed, but on the other hand side Model and its
     * related CardModel and FieldModel are not reloaded again and again. This Map uses the Model.id field as key
     */
    private static HashMap<Long, Model> sModels = new HashMap<Long, Model>();

    /**
     * As above but mapping from CardModel to related Model (because when one has a Card, then you need to jump from
     * CardModel to Model.
     */
    private static HashMap<Long, Model> sCardModelToModelMap = new HashMap<Long, Model>();

    // BEGIN SQL table entries
    private long mId; // Primary key
    private long mDeckId; // Foreign key
    private double mCreated = Utils.now();
    private double mModified = Utils.now();
    private String mTags = "";
    private String mName;
    private String mDescription = "";
    private String mFeatures = ""; // used as the media url
    private double mSpacing = 0.1; // obsolete as of libanki 1.1.4
    private double mInitialSpacing = 60; // obsolete as of libanki 1.1.4
    private int mSource = 0;
    // BEGIN SQL table entries

    private Deck mDeck;

    /** Map for convenience and speed which contains CardModels from current model */
    private LinkedHashMap<Long, CardModel> mCardModelsMap = new LinkedHashMap<Long, CardModel>();

    /** Map for convenience and speed which contains FieldModels from current model */
    private TreeMap<Long, FieldModel> mFieldModelsMap = new TreeMap<Long, FieldModel>();

    /** Map for convenience and speed which contains the CSS code related to a CardModel */
    private HashMap<Long, String> mCssCardModelMap = new HashMap<Long, String>();

    private HashMap<Long, String> mColorCardModelMap = new HashMap<Long, String>();

    /**
     * The percentage chosen in preferences for font sizing at the time when the css for the CardModels related to this
     * Model was calculated in prepareCSSForCardModels.
     */
    private transient int mDisplayPercentage = 0;

    private boolean mInvertedColor = false;

    private Model(Deck deck, String name) {
        mDeck = deck;
        mName = name;
        mId = Utils.genID();
    }


    private Model(Deck deck) {
        this(deck, "");
    }


    // XXX: Unused
    // public void setModified() {
    // mModified = Utils.now();
    // }

    /**
     * FIXME: this should be called whenever the deck is changed. Otherwise unnecessary space will be used. XXX: Unused
     */
    protected static final void reset() {
        sModels = new HashMap<Long, Model>();
        sCardModelToModelMap = new HashMap<Long, Model>();
    }


    /**
     * Returns a Model based on the submitted identifier. If a model id is submitted (isModelId = true), then the Model
     * data and all related CardModel and FieldModel data are loaded, unless the id is the same as one of the
     * currentModel. If a cardModel id is submitted, then the related Model data and all related CardModel and
     * FieldModel data are loaded unless the cardModel id is already in the cardModel map. FIXME: nothing is done to
     * treat db failure or non-existing identifiers
     * 
     * @param deck The deck we are working with
     * @param identifier a cardModel id or a model id
     * @param isModelId if true then the submitted identifier is a model id; otherwise the identifier is a cardModel id
     * @return
     */
    protected static Model getModel(Deck deck, long identifier, boolean isModelId) {
        if (!isModelId) {
            // check whether the identifier is in the cardModelToModelMap
            if (!sCardModelToModelMap.containsKey(identifier)) {
                // get the modelId
                long myModelId = CardModel.modelIdFromDB(deck, identifier);
                // get the model
                loadFromDBPlusRelatedModels(deck, myModelId);
            }
            return sCardModelToModelMap.get(identifier);
        }
        // else it is a modelId
        if (!sModels.containsKey(identifier)) {
            // get the model
            loadFromDBPlusRelatedModels(deck, identifier);
        }
        return sModels.get(identifier);
    }


    public static HashMap<Long, Model> getModels(Deck deck) {
        Model mModel;
        HashMap<Long, Model> mModels = new HashMap<Long, Model>();

        ResultSet result = null;
        AnkiDb ankiDB = deck.getDB();
        try {
        	result = ankiDB.rawQuery("SELECT id FROM models");
            while (result.next()) {
                Long id = result.getLong(1);
                mModel = getModel(deck, id, true);
                mModels.put(id, mModel);
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
        return mModels;
    }


    public TreeMap<Long, FieldModel> getFieldModels() {
        TreeMap<Long, FieldModel> mFieldModels = new TreeMap<Long, FieldModel>();
        FieldModel.fromDb(mDeck, mId, mFieldModels);
        return mFieldModels;

    }


    public List<CardModel> getCardModels() {
        return new ArrayList<CardModel>(mCardModelsMap.values());
    }


    protected final CardModel getCardModel(long identifier) {
        return mCardModelsMap.get(identifier);
    }


    /**
     * Loads the Model from the database. then loads the related CardModels and FieldModels from the database.
     *
     * @param deck
     * @param modelId
     */
    private static void loadFromDBPlusRelatedModels(Deck deck, long modelId) {
        Model currentModel = fromDb(deck, modelId);

        // load related card models
        CardModel.fromDb(deck, currentModel.mId, currentModel.mCardModelsMap);

        // load related field models
        FieldModel.fromDb(deck, modelId, currentModel.mFieldModelsMap);

        // make relations to maps
        sModels.put(currentModel.mId, currentModel);
        CardModel myCardModel = null;
        for (Map.Entry<Long, CardModel> entry : currentModel.mCardModelsMap.entrySet()) {
            myCardModel = entry.getValue();
            sCardModelToModelMap.put(myCardModel.getId(), currentModel);
        }
    }


    protected void saveToDBPlusRelatedModels(Deck deck) {
        for (CardModel cm : mCardModelsMap.values()) {
            cm.toDB(deck);
        }
        for (FieldModel fm : mFieldModelsMap.values()) {
            fm.toDB(deck);
        }
        toDB(deck);
    }


    protected void toDB(Deck deck) {
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("id", mId);
        values.put("deckid", mDeckId);
        values.put("created", mCreated);
        values.put("modified", mModified);
        values.put("tags", mTags);
        values.put("name", mName);
        values.put("description", mDescription);
        values.put("features", mFeatures);
        values.put("spacing", mSpacing);
        values.put("initialSpacing", mInitialSpacing);
        values.put("source", mSource);
        deck.getDB().update(deck, "models", values, "id = " + mId);
    }


    /**
     * Loads a model from the database based on the id. FIXME: nothing is done in case of db error or no returned row
     * 
     * @param deck
     * @param id
     * @return
     */
    private static Model fromDb(Deck deck, long id) {
        ResultSet result = null;
        Model model = null;
        try {
            StringBuffer query = new StringBuffer();
            query.append("SELECT id, deckId, created, modified, tags, name, description");
            query.append(", features, spacing, initialSpacing, source");
            query.append(" FROM models");
            query.append(" WHERE id = ").append(id);
            result = deck.getDB().rawQuery(query.toString());

            if (result.next()) {
	            model = new Model(deck);
            }

            int i = 1;
	        model.mId = result.getLong(i++); // Primary key
	        model.mDeckId = result.getLong(i++); // Foreign key
	        model.mCreated = result.getDouble(i++);
	        model.mModified = result.getDouble(i++);
	        model.mTags = result.getString(i++);
	        model.mName = result.getString(i++);
	        model.mDescription = result.getString(i++);
	        model.mFeatures = result.getString(i++);
	        model.mSpacing = result.getDouble(i++);
	        model.mInitialSpacing = result.getDouble(i++);
	        model.mSource = result.getInt(i++);
            
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
        return model;
    }


    /**
     * @return the ID
     */
    public long getId() {
        return mId;
    }


    /**
     * Prepares the CSS for all CardModels in this Model
     */
    /*
    private void prepareCSSForCardModels(boolean invertedColors, int nightModeBackground) {
        CardModel myCardModel = null;
        String cssString = null;
        for (Map.Entry<Long, CardModel> entry : mCardModelsMap.entrySet()) {
            myCardModel = entry.getValue();
            cssString = createCSSForFontColorSize(myCardModel.getId(), mDisplayPercentage, invertedColors, nightModeBackground);
            mCssCardModelMap.put(myCardModel.getId(), cssString);
        }
    } */


    /**
     * Prepares the Background Colors for all CardModels in this Model
     */
    /*
    private void prepareColorForCardModels(boolean invertedColors, int nightModeBackground) {
        CardModel myCardModel = null;
        String color = null;
        mColorCardModelMap.clear();
        for (Map.Entry<Long, CardModel> entry : mCardModelsMap.entrySet()) {
            myCardModel = entry.getValue();
            color = invertColor(myCardModel.getLastFontColour(), invertedColors);
        	if (nightModeBackground != Color.BLACK && Color.parseColor(color) == Color.BLACK) {
        		color = String.format("#%X", nightModeBackground);
        	}
            mColorCardModelMap.put(myCardModel.getId(), color);
        }
    } */


    /**
     * Returns a cached CSS for the font color and font size of a given CardModel taking into account the included
     * fields
     *
     * @param myCardModelId
     * @param percentage the preference factor to use for calculating the display font size from the cardmodel and
     *            fontmodel font size
     * @return the html contents surrounded by a css style which contains class styles for answer/question and fields
     */
    /*
    protected final String getCSSForFontColorSize(long myCardModelId, int percentage, boolean invertedColors, int nightModeBackground) {
        // If the percentage or night mode has changed, prepare for them.
        if (mDisplayPercentage != percentage || mInvertedColor != invertedColors) {
            mDisplayPercentage = percentage;
            mInvertedColor = invertedColors;
            prepareColorForCardModels(invertedColors, nightModeBackground);
            prepareCSSForCardModels(invertedColors, nightModeBackground);
        }
        return mCssCardModelMap.get(myCardModelId);
    } */

    /*
    protected final int getBackgroundColor(long myCardModelId) {
		String color = mColorCardModelMap.get(myCardModelId);
		if (color != null) {
			return Color.parseColor(color);
		} else {
			return Color.WHITE;
        }
    } */


    /**
     * @param myCardModelId
     * @param percentage the factor to apply to the font size in card model to the display size (in %)
     * @return the html contents surrounded by a css style which contains class styles for answer/question and fields
     */
    /*
    private String createCSSForFontColorSize(long myCardModelId, int percentage, boolean invertedColors, int nightModeBackground) {
        StringBuffer sb = new StringBuffer();
        sb.append("<!-- ").append(percentage).append(" % display font size-->");
        sb.append("<style type=\"text/css\">\n");
        CardModel myCardModel = mCardModelsMap.get(myCardModelId);

        // body background
        if (null != myCardModel.getLastFontColour() && 0 < myCardModel.getLastFontColour().trim().length()) {
        	String color = invertColor(myCardModel.getLastFontColour(), invertedColors);
        	if (nightModeBackground != Color.BLACK && Color.parseColor(color) == Color.BLACK) {
        		color = String.format("#%X", nightModeBackground);
        	}
            sb.append("body {background-color:").append(color).append(";}\n");
        }
        // question
        sb.append(".").append(Reviewer.QUESTION_CLASS).append(" {\n");
        sb.append(calculateDisplay(percentage, myCardModel.getQuestionFontFamily(), myCardModel.getQuestionFontSize(),
                myCardModel.getQuestionFontColour(), myCardModel.getQuestionAlign(), false, invertedColors));
        sb.append("}\n");
        // answer (alignment is stored in question as alignment is shared in question and answer)
        sb.append(".").append(Reviewer.ANSWER_CLASS).append(" {\n");
        sb.append(calculateDisplay(percentage, myCardModel.getAnswerFontFamily(), myCardModel.getAnswerFontSize(),
                myCardModel.getAnswerFontColour(), myCardModel.getQuestionAlign(), false, invertedColors));
        sb.append("}\n");
        // css for fields. Gets css for all fields no matter whether they actually are used in a given card model
        FieldModel myFieldModel = null;
        String hexId = null; // a FieldModel id in unsigned hexa code for the class attribute
        for (Map.Entry<Long, FieldModel> entry : mFieldModelsMap.entrySet()) {
            myFieldModel = entry.getValue();
            hexId = "fm" + Long.toHexString(myFieldModel.getId());
            sb.append(".").append(hexId).append(" {\n");
            sb.append(calculateDisplay(percentage, myFieldModel.getQuizFontFamily(), myFieldModel.getQuizFontSize(),
                    myFieldModel.getQuizFontColour(), 0, true, invertedColors));
            sb.append("}\n");
        }

        // finish
        sb.append("</style>");
        return sb.toString();
    } */


    /**
     * Returns a string where all colors have been inverted.
     * It applies to anything that is in a tag and looks like #FFFFFF
     * 
     * Example: Here only #000000 will be replaced (#777777 is content)
     * <span style="color: #000000;">Code #777777 is the grey color</span>
     * 
     * This is done with a state machine with 2 states:
     *  - 0: within content
     *  - 1: within a tag
     */
    public static String invertColors(String text, boolean invert) {
        if (invert) {
            int state = 0;
            StringBuffer inverted = new StringBuffer(text.length());
            for(int i=0; i<text.length(); i++) {
                char character = text.charAt(i);
                if (state == 1 && character == '#') {
                    inverted.append(invertColor(text.substring(i+1, i+7), true));
                }
                else {
                    if (character == '<') {
                        state = 1;
                    }
                    if (character == '>') {
                        state = 0;
                    }
                    inverted.append(character);
                }
            }
            return inverted.toString();
        }
        else {
            return text;
        }
    }
    

    private static String invertColor(String color, boolean invert) {
    	if (invert) {
    	    if (color != null) {
    	        color = StringTools.toUpperCase(color);
    	    }
            final char[] items = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
            final char[] tmpItems = {'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v'};
            for (int i = 0; i < 16; i++) {
                color = color.replace(items[i], tmpItems[15-i]);
            }
            for (int i = 0; i < 16; i++) {
                color = color.replace(tmpItems[i], items[i]);
            }
		}
		return color;		
    }


    private static String calculateDisplay(int percentage, String fontFamily, int fontSize, String fontColour,
            int align, boolean isField, boolean invertedColors) {
        StringBuffer sb = new StringBuffer();
        if (null != fontFamily && 0 < fontFamily.trim().length()) {
            sb.append("font-family:\"").append(fontFamily).append("\";\n");
        }
        if (null != fontColour && 0 < fontColour.trim().length()) {
            sb.append("color:").append(invertColor(fontColour, invertedColors)).append(";\n");
        }
        if (0 < fontSize) {
            sb.append("font-size:");
            sb.append((percentage * fontSize) / 100);
            sb.append("px;\n");
        }

        if (!isField) {
            sb.append("text-align:");
            sb.append(align_text[align]);
            sb.append(";\n");
            sb.append("padding-left:5px;\n");
            sb.append("padding-right:5px;\n");
        }

        return sb.toString();
    }


    /**
     * @return the name
     */
    public String getName() {
        return mName;
    }


    /**
     * @return the tags
     */
    public String getTags() {
        return mTags;
    }


    public String getFeatures() {
        return mFeatures;
    }

    public Boolean hasTag(String tag) {
    	if(mTags==null || mTags.equals(""))
    		return false;
    		
		if(mTags.equals(tag))
			return true;
		
    	return Arrays.asList(mTags.split(" ")).contains(tag);
    }
}
