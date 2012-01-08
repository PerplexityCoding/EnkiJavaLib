package com.ichi2.anki.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Field {
	
	public static Logger log = LoggerFactory.getLogger(Field.class);
	
    /**
     * FIXME: nothing is done in case of db error or no returned row
     * 
     * @param factId
     * @param fieldModelId
     * @return the value of a field corresponding to the 2 search parameters - or an empty string if not found
     */
    protected final static String fieldValuefromDb(Deck deck, long factId, long fieldModelId) {
        ResultSet result = null;
        String value = "";
        try {
            StringBuffer query = new StringBuffer();
            query.append("SELECT value");
            query.append(" FROM fields");
            query.append(" WHERE factId = ").append(factId).append(" AND fieldModelId = ").append(fieldModelId);
            result = deck.getDB().rawQuery(query.toString());

            if (result.next()) {
            	value = result.getString(1); // Primary key
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
        return value;
    }
}
