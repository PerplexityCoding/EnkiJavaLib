package com.ichi2.anki.utils;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

public class ConnectionAwareResultSet implements ResultSet {

	public ResultSet result;
	public Connection conn;

	public ConnectionAwareResultSet(ResultSet result, Connection conn) {
		super();
		this.result = result;
		this.conn = conn;
	}

	public boolean absolute(int row) throws SQLException {
		return result.absolute(row);
	}

	public void afterLast() throws SQLException {
		result.afterLast();
	}

	public void beforeFirst() throws SQLException {
		result.beforeFirst();
	}

	public void cancelRowUpdates() throws SQLException {
		result.cancelRowUpdates();
	}

	public void clearWarnings() throws SQLException {
		result.clearWarnings();
	}

	public void close() throws SQLException {
		result.close();
		// CLOSE CONNECTION
		conn.close();
	}

	public void deleteRow() throws SQLException {
		result.deleteRow();
	}

	public int findColumn(String columnLabel) throws SQLException {
		return result.findColumn(columnLabel);
	}

	public boolean first() throws SQLException {
		return result.first();
	}

	public Array getArray(int columnIndex) throws SQLException {
		return result.getArray(columnIndex);
	}

	public Array getArray(String columnLabel) throws SQLException {
		return result.getArray(columnLabel);
	}

	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		return result.getAsciiStream(columnIndex);
	}

	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		return result.getAsciiStream(columnLabel);
	}

	@Deprecated
	public BigDecimal getBigDecimal(int columnIndex, int scale)
			throws SQLException {
		return result.getBigDecimal(columnIndex, scale);
	}

	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		return result.getBigDecimal(columnIndex);
	}

	@Deprecated
	public BigDecimal getBigDecimal(String columnLabel, int scale)
			throws SQLException {
		return result.getBigDecimal(columnLabel, scale);
	}

	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		return result.getBigDecimal(columnLabel);
	}

	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		return result.getBinaryStream(columnIndex);
	}

	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		return result.getBinaryStream(columnLabel);
	}

	public Blob getBlob(int columnIndex) throws SQLException {
		return result.getBlob(columnIndex);
	}

	public Blob getBlob(String columnLabel) throws SQLException {
		return result.getBlob(columnLabel);
	}

	public boolean getBoolean(int columnIndex) throws SQLException {
		return result.getBoolean(columnIndex);
	}

	public boolean getBoolean(String columnLabel) throws SQLException {
		return result.getBoolean(columnLabel);
	}

	public byte getByte(int columnIndex) throws SQLException {
		return result.getByte(columnIndex);
	}

	public byte getByte(String columnLabel) throws SQLException {
		return result.getByte(columnLabel);
	}

	public byte[] getBytes(int columnIndex) throws SQLException {
		return result.getBytes(columnIndex);
	}

	public byte[] getBytes(String columnLabel) throws SQLException {
		return result.getBytes(columnLabel);
	}

	public Reader getCharacterStream(int columnIndex) throws SQLException {
		return result.getCharacterStream(columnIndex);
	}

	public Reader getCharacterStream(String columnLabel) throws SQLException {
		return result.getCharacterStream(columnLabel);
	}

	public Clob getClob(int columnIndex) throws SQLException {
		return result.getClob(columnIndex);
	}

	public Clob getClob(String columnLabel) throws SQLException {
		return result.getClob(columnLabel);
	}

	public int getConcurrency() throws SQLException {
		return result.getConcurrency();
	}

	public String getCursorName() throws SQLException {
		return result.getCursorName();
	}

	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		return result.getDate(columnIndex, cal);
	}

	public Date getDate(int columnIndex) throws SQLException {
		return result.getDate(columnIndex);
	}

	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		return result.getDate(columnLabel, cal);
	}

	public Date getDate(String columnLabel) throws SQLException {
		return result.getDate(columnLabel);
	}

	public double getDouble(int columnIndex) throws SQLException {
		return result.getDouble(columnIndex);
	}

	public double getDouble(String columnLabel) throws SQLException {
		return result.getDouble(columnLabel);
	}

	public int getFetchDirection() throws SQLException {
		return result.getFetchDirection();
	}

	public int getFetchSize() throws SQLException {
		return result.getFetchSize();
	}

	public float getFloat(int columnIndex) throws SQLException {
		return result.getFloat(columnIndex);
	}

	public float getFloat(String columnLabel) throws SQLException {
		return result.getFloat(columnLabel);
	}

	public int getHoldability() throws SQLException {
		return result.getHoldability();
	}

	public int getInt(int columnIndex) throws SQLException {
		return result.getInt(columnIndex);
	}

	public int getInt(String columnLabel) throws SQLException {
		return result.getInt(columnLabel);
	}

	public long getLong(int columnIndex) throws SQLException {
		return result.getLong(columnIndex);
	}

	public long getLong(String columnLabel) throws SQLException {
		return result.getLong(columnLabel);
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		return result.getMetaData();
	}

	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		return result.getNCharacterStream(columnIndex);
	}

	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		return result.getNCharacterStream(columnLabel);
	}

	public NClob getNClob(int columnIndex) throws SQLException {
		return result.getNClob(columnIndex);
	}

	public NClob getNClob(String columnLabel) throws SQLException {
		return result.getNClob(columnLabel);
	}

	public String getNString(int columnIndex) throws SQLException {
		return result.getNString(columnIndex);
	}

	public String getNString(String columnLabel) throws SQLException {
		return result.getNString(columnLabel);
	}

	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		return result.getObject(columnIndex, type);
	}

	public Object getObject(int columnIndex, Map<String, Class<?>> map)
			throws SQLException {
		return result.getObject(columnIndex, map);
	}

	public Object getObject(int columnIndex) throws SQLException {
		return result.getObject(columnIndex);
	}

	public <T> T getObject(String columnLabel, Class<T> type)
			throws SQLException {
		return result.getObject(columnLabel, type);
	}

	public Object getObject(String columnLabel, Map<String, Class<?>> map)
			throws SQLException {
		return result.getObject(columnLabel, map);
	}

	public Object getObject(String columnLabel) throws SQLException {
		return result.getObject(columnLabel);
	}

	public Ref getRef(int columnIndex) throws SQLException {
		return result.getRef(columnIndex);
	}

	public Ref getRef(String columnLabel) throws SQLException {
		return result.getRef(columnLabel);
	}

	public int getRow() throws SQLException {
		return result.getRow();
	}

	public RowId getRowId(int columnIndex) throws SQLException {
		return result.getRowId(columnIndex);
	}

	public RowId getRowId(String columnLabel) throws SQLException {
		return result.getRowId(columnLabel);
	}

	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		return result.getSQLXML(columnIndex);
	}

	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		return result.getSQLXML(columnLabel);
	}

	public short getShort(int columnIndex) throws SQLException {
		return result.getShort(columnIndex);
	}

	public short getShort(String columnLabel) throws SQLException {
		return result.getShort(columnLabel);
	}

	public Statement getStatement() throws SQLException {
		return result.getStatement();
	}

	public String getString(int columnIndex) throws SQLException {
		return result.getString(columnIndex);
	}

	public String getString(String columnLabel) throws SQLException {
		return result.getString(columnLabel);
	}

	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		return result.getTime(columnIndex, cal);
	}

	public Time getTime(int columnIndex) throws SQLException {
		return result.getTime(columnIndex);
	}

	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		return result.getTime(columnLabel, cal);
	}

	public Time getTime(String columnLabel) throws SQLException {
		return result.getTime(columnLabel);
	}

	public Timestamp getTimestamp(int columnIndex, Calendar cal)
			throws SQLException {
		return result.getTimestamp(columnIndex, cal);
	}

	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return result.getTimestamp(columnIndex);
	}

	public Timestamp getTimestamp(String columnLabel, Calendar cal)
			throws SQLException {
		return result.getTimestamp(columnLabel, cal);
	}

	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		return result.getTimestamp(columnLabel);
	}

	public int getType() throws SQLException {
		return result.getType();
	}

	public URL getURL(int columnIndex) throws SQLException {
		return result.getURL(columnIndex);
	}

	public URL getURL(String columnLabel) throws SQLException {
		return result.getURL(columnLabel);
	}

	@Deprecated
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		return result.getUnicodeStream(columnIndex);
	}

	@Deprecated
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		return result.getUnicodeStream(columnLabel);
	}

	public SQLWarning getWarnings() throws SQLException {
		return result.getWarnings();
	}

	public void insertRow() throws SQLException {
		result.insertRow();
	}

	public boolean isAfterLast() throws SQLException {
		return result.isAfterLast();
	}

	public boolean isBeforeFirst() throws SQLException {
		return result.isBeforeFirst();
	}

	public boolean isClosed() throws SQLException {
		return result.isClosed();
	}

	public boolean isFirst() throws SQLException {
		return result.isFirst();
	}

	public boolean isLast() throws SQLException {
		return result.isLast();
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return result.isWrapperFor(iface);
	}

	public boolean last() throws SQLException {
		return result.last();
	}

	public void moveToCurrentRow() throws SQLException {
		result.moveToCurrentRow();
	}

	public void moveToInsertRow() throws SQLException {
		result.moveToInsertRow();
	}

	public boolean next() throws SQLException {
		return result.next();
	}

	public boolean previous() throws SQLException {
		return result.previous();
	}

	public void refreshRow() throws SQLException {
		result.refreshRow();
	}

	public boolean relative(int rows) throws SQLException {
		return result.relative(rows);
	}

	public boolean rowDeleted() throws SQLException {
		return result.rowDeleted();
	}

	public boolean rowInserted() throws SQLException {
		return result.rowInserted();
	}

	public boolean rowUpdated() throws SQLException {
		return result.rowUpdated();
	}

	public void setFetchDirection(int direction) throws SQLException {
		result.setFetchDirection(direction);
	}

	public void setFetchSize(int rows) throws SQLException {
		result.setFetchSize(rows);
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		return result.unwrap(iface);
	}

	public void updateArray(int columnIndex, Array x) throws SQLException {
		result.updateArray(columnIndex, x);
	}

	public void updateArray(String columnLabel, Array x) throws SQLException {
		result.updateArray(columnLabel, x);
	}

	public void updateAsciiStream(int columnIndex, InputStream x, int length)
			throws SQLException {
		result.updateAsciiStream(columnIndex, x, length);
	}

	public void updateAsciiStream(int columnIndex, InputStream x, long length)
			throws SQLException {
		result.updateAsciiStream(columnIndex, x, length);
	}

	public void updateAsciiStream(int columnIndex, InputStream x)
			throws SQLException {
		result.updateAsciiStream(columnIndex, x);
	}

	public void updateAsciiStream(String columnLabel, InputStream x, int length)
			throws SQLException {
		result.updateAsciiStream(columnLabel, x, length);
	}

	public void updateAsciiStream(String columnLabel, InputStream x, long length)
			throws SQLException {
		result.updateAsciiStream(columnLabel, x, length);
	}

	public void updateAsciiStream(String columnLabel, InputStream x)
			throws SQLException {
		result.updateAsciiStream(columnLabel, x);
	}

	public void updateBigDecimal(int columnIndex, BigDecimal x)
			throws SQLException {
		result.updateBigDecimal(columnIndex, x);
	}

	public void updateBigDecimal(String columnLabel, BigDecimal x)
			throws SQLException {
		result.updateBigDecimal(columnLabel, x);
	}

	public void updateBinaryStream(int columnIndex, InputStream x, int length)
			throws SQLException {
		result.updateBinaryStream(columnIndex, x, length);
	}

	public void updateBinaryStream(int columnIndex, InputStream x, long length)
			throws SQLException {
		result.updateBinaryStream(columnIndex, x, length);
	}

	public void updateBinaryStream(int columnIndex, InputStream x)
			throws SQLException {
		result.updateBinaryStream(columnIndex, x);
	}

	public void updateBinaryStream(String columnLabel, InputStream x, int length)
			throws SQLException {
		result.updateBinaryStream(columnLabel, x, length);
	}

	public void updateBinaryStream(String columnLabel, InputStream x,
			long length) throws SQLException {
		result.updateBinaryStream(columnLabel, x, length);
	}

	public void updateBinaryStream(String columnLabel, InputStream x)
			throws SQLException {
		result.updateBinaryStream(columnLabel, x);
	}

	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		result.updateBlob(columnIndex, x);
	}

	public void updateBlob(int columnIndex, InputStream inputStream, long length)
			throws SQLException {
		result.updateBlob(columnIndex, inputStream, length);
	}

	public void updateBlob(int columnIndex, InputStream inputStream)
			throws SQLException {
		result.updateBlob(columnIndex, inputStream);
	}

	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		result.updateBlob(columnLabel, x);
	}

	public void updateBlob(String columnLabel, InputStream inputStream,
			long length) throws SQLException {
		result.updateBlob(columnLabel, inputStream, length);
	}

	public void updateBlob(String columnLabel, InputStream inputStream)
			throws SQLException {
		result.updateBlob(columnLabel, inputStream);
	}

	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		result.updateBoolean(columnIndex, x);
	}

	public void updateBoolean(String columnLabel, boolean x)
			throws SQLException {
		result.updateBoolean(columnLabel, x);
	}

	public void updateByte(int columnIndex, byte x) throws SQLException {
		result.updateByte(columnIndex, x);
	}

	public void updateByte(String columnLabel, byte x) throws SQLException {
		result.updateByte(columnLabel, x);
	}

	public void updateBytes(int columnIndex, byte[] x) throws SQLException {
		result.updateBytes(columnIndex, x);
	}

	public void updateBytes(String columnLabel, byte[] x) throws SQLException {
		result.updateBytes(columnLabel, x);
	}

	public void updateCharacterStream(int columnIndex, Reader x, int length)
			throws SQLException {
		result.updateCharacterStream(columnIndex, x, length);
	}

	public void updateCharacterStream(int columnIndex, Reader x, long length)
			throws SQLException {
		result.updateCharacterStream(columnIndex, x, length);
	}

	public void updateCharacterStream(int columnIndex, Reader x)
			throws SQLException {
		result.updateCharacterStream(columnIndex, x);
	}

	public void updateCharacterStream(String columnLabel, Reader reader,
			int length) throws SQLException {
		result.updateCharacterStream(columnLabel, reader, length);
	}

	public void updateCharacterStream(String columnLabel, Reader reader,
			long length) throws SQLException {
		result.updateCharacterStream(columnLabel, reader, length);
	}

	public void updateCharacterStream(String columnLabel, Reader reader)
			throws SQLException {
		result.updateCharacterStream(columnLabel, reader);
	}

	public void updateClob(int columnIndex, Clob x) throws SQLException {
		result.updateClob(columnIndex, x);
	}

	public void updateClob(int columnIndex, Reader reader, long length)
			throws SQLException {
		result.updateClob(columnIndex, reader, length);
	}

	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		result.updateClob(columnIndex, reader);
	}

	public void updateClob(String columnLabel, Clob x) throws SQLException {
		result.updateClob(columnLabel, x);
	}

	public void updateClob(String columnLabel, Reader reader, long length)
			throws SQLException {
		result.updateClob(columnLabel, reader, length);
	}

	public void updateClob(String columnLabel, Reader reader)
			throws SQLException {
		result.updateClob(columnLabel, reader);
	}

	public void updateDate(int columnIndex, Date x) throws SQLException {
		result.updateDate(columnIndex, x);
	}

	public void updateDate(String columnLabel, Date x) throws SQLException {
		result.updateDate(columnLabel, x);
	}

	public void updateDouble(int columnIndex, double x) throws SQLException {
		result.updateDouble(columnIndex, x);
	}

	public void updateDouble(String columnLabel, double x) throws SQLException {
		result.updateDouble(columnLabel, x);
	}

	public void updateFloat(int columnIndex, float x) throws SQLException {
		result.updateFloat(columnIndex, x);
	}

	public void updateFloat(String columnLabel, float x) throws SQLException {
		result.updateFloat(columnLabel, x);
	}

	public void updateInt(int columnIndex, int x) throws SQLException {
		result.updateInt(columnIndex, x);
	}

	public void updateInt(String columnLabel, int x) throws SQLException {
		result.updateInt(columnLabel, x);
	}

	public void updateLong(int columnIndex, long x) throws SQLException {
		result.updateLong(columnIndex, x);
	}

	public void updateLong(String columnLabel, long x) throws SQLException {
		result.updateLong(columnLabel, x);
	}

	public void updateNCharacterStream(int columnIndex, Reader x, long length)
			throws SQLException {
		result.updateNCharacterStream(columnIndex, x, length);
	}

	public void updateNCharacterStream(int columnIndex, Reader x)
			throws SQLException {
		result.updateNCharacterStream(columnIndex, x);
	}

	public void updateNCharacterStream(String columnLabel, Reader reader,
			long length) throws SQLException {
		result.updateNCharacterStream(columnLabel, reader, length);
	}

	public void updateNCharacterStream(String columnLabel, Reader reader)
			throws SQLException {
		result.updateNCharacterStream(columnLabel, reader);
	}

	public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
		result.updateNClob(columnIndex, nClob);
	}

	public void updateNClob(int columnIndex, Reader reader, long length)
			throws SQLException {
		result.updateNClob(columnIndex, reader, length);
	}

	public void updateNClob(int columnIndex, Reader reader) throws SQLException {
		result.updateNClob(columnIndex, reader);
	}

	public void updateNClob(String columnLabel, NClob nClob)
			throws SQLException {
		result.updateNClob(columnLabel, nClob);
	}

	public void updateNClob(String columnLabel, Reader reader, long length)
			throws SQLException {
		result.updateNClob(columnLabel, reader, length);
	}

	public void updateNClob(String columnLabel, Reader reader)
			throws SQLException {
		result.updateNClob(columnLabel, reader);
	}

	public void updateNString(int columnIndex, String nString)
			throws SQLException {
		result.updateNString(columnIndex, nString);
	}

	public void updateNString(String columnLabel, String nString)
			throws SQLException {
		result.updateNString(columnLabel, nString);
	}

	public void updateNull(int columnIndex) throws SQLException {
		result.updateNull(columnIndex);
	}

	public void updateNull(String columnLabel) throws SQLException {
		result.updateNull(columnLabel);
	}

	public void updateObject(int columnIndex, Object x, int scaleOrLength)
			throws SQLException {
		result.updateObject(columnIndex, x, scaleOrLength);
	}

	public void updateObject(int columnIndex, Object x) throws SQLException {
		result.updateObject(columnIndex, x);
	}

	public void updateObject(String columnLabel, Object x, int scaleOrLength)
			throws SQLException {
		result.updateObject(columnLabel, x, scaleOrLength);
	}

	public void updateObject(String columnLabel, Object x) throws SQLException {
		result.updateObject(columnLabel, x);
	}

	public void updateRef(int columnIndex, Ref x) throws SQLException {
		result.updateRef(columnIndex, x);
	}

	public void updateRef(String columnLabel, Ref x) throws SQLException {
		result.updateRef(columnLabel, x);
	}

	public void updateRow() throws SQLException {
		result.updateRow();
	}

	public void updateRowId(int columnIndex, RowId x) throws SQLException {
		result.updateRowId(columnIndex, x);
	}

	public void updateRowId(String columnLabel, RowId x) throws SQLException {
		result.updateRowId(columnLabel, x);
	}

	public void updateSQLXML(int columnIndex, SQLXML xmlObject)
			throws SQLException {
		result.updateSQLXML(columnIndex, xmlObject);
	}

	public void updateSQLXML(String columnLabel, SQLXML xmlObject)
			throws SQLException {
		result.updateSQLXML(columnLabel, xmlObject);
	}

	public void updateShort(int columnIndex, short x) throws SQLException {
		result.updateShort(columnIndex, x);
	}

	public void updateShort(String columnLabel, short x) throws SQLException {
		result.updateShort(columnLabel, x);
	}

	public void updateString(int columnIndex, String x) throws SQLException {
		result.updateString(columnIndex, x);
	}

	public void updateString(String columnLabel, String x) throws SQLException {
		result.updateString(columnLabel, x);
	}

	public void updateTime(int columnIndex, Time x) throws SQLException {
		result.updateTime(columnIndex, x);
	}

	public void updateTime(String columnLabel, Time x) throws SQLException {
		result.updateTime(columnLabel, x);
	}

	public void updateTimestamp(int columnIndex, Timestamp x)
			throws SQLException {
		result.updateTimestamp(columnIndex, x);
	}

	public void updateTimestamp(String columnLabel, Timestamp x)
			throws SQLException {
		result.updateTimestamp(columnLabel, x);
	}

	public boolean wasNull() throws SQLException {
		return result.wasNull();
	}
	
	
}
