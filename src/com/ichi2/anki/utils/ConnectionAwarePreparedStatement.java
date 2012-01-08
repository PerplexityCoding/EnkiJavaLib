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
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

public class ConnectionAwarePreparedStatement implements PreparedStatement {

	private PreparedStatement statement;
	private Connection conn;
	
	public ConnectionAwarePreparedStatement(PreparedStatement statement, Connection conn) {
		this.statement = statement;
		this.conn = conn;
	}
	
	public void addBatch() throws SQLException {
		statement.addBatch();
	}
	public void addBatch(String arg0) throws SQLException {
		statement.addBatch(arg0);
	}
	public void cancel() throws SQLException {
		statement.cancel();
	}
	public void clearBatch() throws SQLException {
		statement.clearBatch();
	}
	public void clearParameters() throws SQLException {
		statement.clearParameters();
	}
	public void clearWarnings() throws SQLException {
		statement.clearWarnings();
	}
	public void close() throws SQLException {
		conn.commit();
		
		statement.close();
		conn.close();
	}
	public void closeOnCompletion() throws SQLException {
		statement.closeOnCompletion();
	}
	public boolean execute() throws SQLException {
		return statement.execute();
	}
	public boolean execute(String arg0, int arg1) throws SQLException {
		return statement.execute(arg0, arg1);
	}
	public boolean execute(String arg0, int[] arg1) throws SQLException {
		return statement.execute(arg0, arg1);
	}
	public boolean execute(String arg0, String[] arg1) throws SQLException {
		return statement.execute(arg0, arg1);
	}
	public boolean execute(String arg0) throws SQLException {
		return statement.execute(arg0);
	}
	public int[] executeBatch() throws SQLException {
		return statement.executeBatch();
	}
	public ResultSet executeQuery() throws SQLException {
		return statement.executeQuery();
	}
	public ResultSet executeQuery(String arg0) throws SQLException {
		return statement.executeQuery(arg0);
	}
	public int executeUpdate() throws SQLException {
		return statement.executeUpdate();
	}
	public int executeUpdate(String arg0, int arg1) throws SQLException {
		return statement.executeUpdate(arg0, arg1);
	}
	public int executeUpdate(String arg0, int[] arg1) throws SQLException {
		return statement.executeUpdate(arg0, arg1);
	}
	public int executeUpdate(String arg0, String[] arg1) throws SQLException {
		return statement.executeUpdate(arg0, arg1);
	}
	public int executeUpdate(String arg0) throws SQLException {
		return statement.executeUpdate(arg0);
	}
	public Connection getConnection() throws SQLException {
		return statement.getConnection();
	}
	public int getFetchDirection() throws SQLException {
		return statement.getFetchDirection();
	}
	public int getFetchSize() throws SQLException {
		return statement.getFetchSize();
	}
	public ResultSet getGeneratedKeys() throws SQLException {
		return statement.getGeneratedKeys();
	}
	public int getMaxFieldSize() throws SQLException {
		return statement.getMaxFieldSize();
	}
	public int getMaxRows() throws SQLException {
		return statement.getMaxRows();
	}
	public ResultSetMetaData getMetaData() throws SQLException {
		return statement.getMetaData();
	}
	public boolean getMoreResults() throws SQLException {
		return statement.getMoreResults();
	}
	public boolean getMoreResults(int arg0) throws SQLException {
		return statement.getMoreResults(arg0);
	}
	public ParameterMetaData getParameterMetaData() throws SQLException {
		return statement.getParameterMetaData();
	}
	public int getQueryTimeout() throws SQLException {
		return statement.getQueryTimeout();
	}
	public ResultSet getResultSet() throws SQLException {
		return statement.getResultSet();
	}
	public int getResultSetConcurrency() throws SQLException {
		return statement.getResultSetConcurrency();
	}
	public int getResultSetHoldability() throws SQLException {
		return statement.getResultSetHoldability();
	}
	public int getResultSetType() throws SQLException {
		return statement.getResultSetType();
	}
	public int getUpdateCount() throws SQLException {
		return statement.getUpdateCount();
	}
	public SQLWarning getWarnings() throws SQLException {
		return statement.getWarnings();
	}
	public boolean isCloseOnCompletion() throws SQLException {
		return statement.isCloseOnCompletion();
	}
	public boolean isClosed() throws SQLException {
		return statement.isClosed();
	}
	public boolean isPoolable() throws SQLException {
		return statement.isPoolable();
	}
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return statement.isWrapperFor(iface);
	}
	public void setArray(int parameterIndex, Array x) throws SQLException {
		statement.setArray(parameterIndex, x);
	}
	public void setAsciiStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		statement.setAsciiStream(parameterIndex, x, length);
	}
	public void setAsciiStream(int parameterIndex, InputStream x, long length)
			throws SQLException {
		statement.setAsciiStream(parameterIndex, x, length);
	}
	public void setAsciiStream(int parameterIndex, InputStream x)
			throws SQLException {
		statement.setAsciiStream(parameterIndex, x);
	}
	public void setBigDecimal(int parameterIndex, BigDecimal x)
			throws SQLException {
		statement.setBigDecimal(parameterIndex, x);
	}
	public void setBinaryStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		statement.setBinaryStream(parameterIndex, x, length);
	}
	public void setBinaryStream(int parameterIndex, InputStream x, long length)
			throws SQLException {
		statement.setBinaryStream(parameterIndex, x, length);
	}
	public void setBinaryStream(int parameterIndex, InputStream x)
			throws SQLException {
		statement.setBinaryStream(parameterIndex, x);
	}
	public void setBlob(int parameterIndex, Blob x) throws SQLException {
		statement.setBlob(parameterIndex, x);
	}
	public void setBlob(int parameterIndex, InputStream inputStream, long length)
			throws SQLException {
		statement.setBlob(parameterIndex, inputStream, length);
	}
	public void setBlob(int parameterIndex, InputStream inputStream)
			throws SQLException {
		statement.setBlob(parameterIndex, inputStream);
	}
	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		statement.setBoolean(parameterIndex, x);
	}
	public void setByte(int parameterIndex, byte x) throws SQLException {
		statement.setByte(parameterIndex, x);
	}
	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		statement.setBytes(parameterIndex, x);
	}
	public void setCharacterStream(int parameterIndex, Reader reader, int length)
			throws SQLException {
		statement.setCharacterStream(parameterIndex, reader, length);
	}
	public void setCharacterStream(int parameterIndex, Reader reader,
			long length) throws SQLException {
		statement.setCharacterStream(parameterIndex, reader, length);
	}
	public void setCharacterStream(int parameterIndex, Reader reader)
			throws SQLException {
		statement.setCharacterStream(parameterIndex, reader);
	}
	public void setClob(int parameterIndex, Clob x) throws SQLException {
		statement.setClob(parameterIndex, x);
	}
	public void setClob(int parameterIndex, Reader reader, long length)
			throws SQLException {
		statement.setClob(parameterIndex, reader, length);
	}
	public void setClob(int parameterIndex, Reader reader) throws SQLException {
		statement.setClob(parameterIndex, reader);
	}
	public void setCursorName(String arg0) throws SQLException {
		statement.setCursorName(arg0);
	}
	public void setDate(int parameterIndex, Date x, Calendar cal)
			throws SQLException {
		statement.setDate(parameterIndex, x, cal);
	}
	public void setDate(int parameterIndex, Date x) throws SQLException {
		statement.setDate(parameterIndex, x);
	}
	public void setDouble(int parameterIndex, double x) throws SQLException {
		statement.setDouble(parameterIndex, x);
	}
	public void setEscapeProcessing(boolean arg0) throws SQLException {
		statement.setEscapeProcessing(arg0);
	}
	public void setFetchDirection(int arg0) throws SQLException {
		statement.setFetchDirection(arg0);
	}
	public void setFetchSize(int arg0) throws SQLException {
		statement.setFetchSize(arg0);
	}
	public void setFloat(int parameterIndex, float x) throws SQLException {
		statement.setFloat(parameterIndex, x);
	}
	public void setInt(int parameterIndex, int x) throws SQLException {
		statement.setInt(parameterIndex, x);
	}
	public void setLong(int parameterIndex, long x) throws SQLException {
		statement.setLong(parameterIndex, x);
	}
	public void setMaxFieldSize(int arg0) throws SQLException {
		statement.setMaxFieldSize(arg0);
	}
	public void setMaxRows(int arg0) throws SQLException {
		statement.setMaxRows(arg0);
	}
	public void setNCharacterStream(int parameterIndex, Reader value,
			long length) throws SQLException {
		statement.setNCharacterStream(parameterIndex, value, length);
	}
	public void setNCharacterStream(int parameterIndex, Reader value)
			throws SQLException {
		statement.setNCharacterStream(parameterIndex, value);
	}
	public void setNClob(int parameterIndex, NClob value) throws SQLException {
		statement.setNClob(parameterIndex, value);
	}
	public void setNClob(int parameterIndex, Reader reader, long length)
			throws SQLException {
		statement.setNClob(parameterIndex, reader, length);
	}
	public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		statement.setNClob(parameterIndex, reader);
	}
	public void setNString(int parameterIndex, String value)
			throws SQLException {
		statement.setNString(parameterIndex, value);
	}
	public void setNull(int parameterIndex, int sqlType, String typeName)
			throws SQLException {
		statement.setNull(parameterIndex, sqlType, typeName);
	}
	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		statement.setNull(parameterIndex, sqlType);
	}
	public void setObject(int parameterIndex, Object x, int targetSqlType,
			int scaleOrLength) throws SQLException {
		statement.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
	}
	public void setObject(int parameterIndex, Object x, int targetSqlType)
			throws SQLException {
		statement.setObject(parameterIndex, x, targetSqlType);
	}
	public void setObject(int parameterIndex, Object x) throws SQLException {
		statement.setObject(parameterIndex, x);
	}
	public void setPoolable(boolean arg0) throws SQLException {
		statement.setPoolable(arg0);
	}
	public void setQueryTimeout(int arg0) throws SQLException {
		statement.setQueryTimeout(arg0);
	}
	public void setRef(int parameterIndex, Ref x) throws SQLException {
		statement.setRef(parameterIndex, x);
	}
	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		statement.setRowId(parameterIndex, x);
	}
	public void setSQLXML(int parameterIndex, SQLXML xmlObject)
			throws SQLException {
		statement.setSQLXML(parameterIndex, xmlObject);
	}
	public void setShort(int parameterIndex, short x) throws SQLException {
		statement.setShort(parameterIndex, x);
	}
	public void setString(int parameterIndex, String x) throws SQLException {
		statement.setString(parameterIndex, x);
	}
	public void setTime(int parameterIndex, Time x, Calendar cal)
			throws SQLException {
		statement.setTime(parameterIndex, x, cal);
	}
	public void setTime(int parameterIndex, Time x) throws SQLException {
		statement.setTime(parameterIndex, x);
	}
	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
			throws SQLException {
		statement.setTimestamp(parameterIndex, x, cal);
	}
	public void setTimestamp(int parameterIndex, Timestamp x)
			throws SQLException {
		statement.setTimestamp(parameterIndex, x);
	}
	public void setURL(int parameterIndex, URL x) throws SQLException {
		statement.setURL(parameterIndex, x);
	}
	@Deprecated
	public void setUnicodeStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		statement.setUnicodeStream(parameterIndex, x, length);
	}
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return statement.unwrap(iface);
	}
	
}
