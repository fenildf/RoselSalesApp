package com.rosel.roselsalesapp.objects;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.rosel.roselsalesapp.Db.DbContract;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class Order implements Serializable{

    public class OrderLine implements Serializable{

        private Order order;
        private Product item;
        private int quantity;
        private float price;
        private float sum;

        public OrderLine(Order order){
            this.order = order;
        }

        public Product getItem() {
            return item;
        }

        public int getQuantity() {
            return quantity;
        }

        public float getPrice() {
            return price;
        }

        public float getSum() {
            return sum;
        }

        public void calcSum(){
            sum = quantity * price;
        }

        public void setItem(Product item) {
            this.item = item;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
            calcSum();
        }

        public void setPrice(float price) {
            this.price = price;
            calcSum();
        }

        public JSONObject toJSONObject(){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("product_id", getItem().getId());
            jsonObject.put("quantity", getQuantity());
            jsonObject.put("price", getPrice());
            jsonObject.put("sum", getSum());
            return jsonObject;
        }

        public ContentValues getContentValues(){
            ContentValues contentValues = new ContentValues();
            contentValues.put(DbContract.Orderlines.COLUMN_NAME_PRODUCT_ID, getItem().getId());
            contentValues.put(DbContract.Orderlines.COLUMN_NAME_QUANTITY, Long.toString(getQuantity()));
            contentValues.put(DbContract.Orderlines.COLUMN_NAME_SUM, Float.toString(getSum()));
            contentValues.put(DbContract.Orderlines.COLUMN_NAME_PRICE, Float.toString(getPrice()));
            contentValues.put(DbContract.Orderlines.COLUMN_NAME_ORDER_ID, Long.toString(order.getOrderId()));
            return contentValues;
        }
    }

    private ArrayList<OrderLine> lines = new ArrayList<>();
    private long orderId;
    private long addressId;
    private String address;
    private Calendar shippingDate = null;
    private Client client = null;
    private boolean isSent = false;
    private String comment;

    private Calendar date = Calendar.getInstance();
    private float sum = 0;

    public float getSum() {
        calcSum();
        return sum;
    }

    public Long getAddressId() {
        if(addressId==0){
            return null;
        }
        return addressId;
    }

    public String getAddress() {
        return address;
    }

    public long getOrderId() {
        return orderId;
    }

    public Calendar getShippingDate() {
        return shippingDate;
    }

    public void setShippingDate(Calendar shippingDate) {
        this.shippingDate = shippingDate;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public void setClient(Client client){
        this.client = client;
    }

    public Client getClient() {
        return client;
    }

    public String getComment() {
        return comment;
    }

    public Calendar getDate() {
        return date;
    }

    public static String getDateString(Calendar date) {
        if(date==null){
            return null;
        }
        return new SimpleDateFormat("dd.MM.yyyy").format(date.getTime());
    }

    public int isSentForSQL(){
        if(isSent) {
            return 1;
        }else{
            return 0;
        }
    }

    public static String getDateForSQL(Calendar dateToConvert){
        if(dateToConvert==null){
            return null;
        }
        String dayString = Integer.toString(dateToConvert.get(Calendar.DAY_OF_MONTH));
        if(dayString.length()==1){
            dayString = "0" + dayString;
        }
        String monthString = Integer.toString(dateToConvert.get(Calendar.MONTH)+1);
        if(monthString.length()==1){
            monthString = "0" + monthString;
        }
        String dateString = Integer.toString(dateToConvert.get(Calendar.YEAR)) + "-" + monthString + "-" + dayString;
        return dateString;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setAddressId(long addressId) {
        this.addressId = addressId;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public static Calendar computeDateFromSQL(String dateFromSQL){
        if(dateFromSQL == null){
            return null;
        }
        Calendar c = new GregorianCalendar();
        String[] s = dateFromSQL.split("-");
        int year = Integer.parseInt(s[0]);
        int month = Integer.parseInt(s[1]);
        int day = Integer.parseInt(s[2]);
        c.set(year, month-1, day);
        return c;
    }

    public void setDate(Calendar date) {
        this.date = date;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void calcSum(){
        sum = 0;
        for(OrderLine line:lines){
            sum += line.getSum();
        }
    }

    public int getLinesCount(){
        return lines.size();
    }

    public OrderLine addLine(){
        OrderLine newLine= new OrderLine(this);
        lines.add(newLine);
        return newLine;
    }

    public void addOrderLine(Product item, int q, float p){
        Order.OrderLine newLine = addLine();
        if(item!=null) {
            newLine.setItem(item);
            newLine.setQuantity(q);
            newLine.setPrice(p);
        }
    }

    public void clearLines(){
        lines.clear();
    }

    public void removeLine(OrderLine orderLine){
        lines.remove(orderLine);
    }

    public OrderLine getLine(int index) {
        return lines.get(index);
    }

    public int getLineIndex(OrderLine line) {
        return lines.indexOf(line);
    }

    public boolean isCorrect(){
        return (getClient()!=null)&&(getLinesCount()>0)&&(getSum()>0)&&(getDate()!=null);
    }

    public Map<Long, Long> getOrdersQuantityMap(){
        Map<Long, Long> result = new HashMap<Long, Long>();
        long linesCount = getLinesCount();
        for(int i=0;i<linesCount;i++){
            Order.OrderLine curLine = getLine(i);
            final long curProductId = curLine.getItem().getId();
            if(result.containsKey(curProductId)){
                long oldValue = result.get(curProductId);
                result.put(curProductId, oldValue + curLine.getQuantity());
            } else {
                result.put(curProductId, (long) curLine.getQuantity());
            }
        }
        return result;
    }

    public boolean checkStock(SQLiteDatabase db){
        Map<Long, Long> orderMap = getOrdersQuantityMap();
        StringBuilder s = new StringBuilder();
        s.append(orderMap.keySet().toString());
        Map<Long, Long> stockMap = getStock(db,s.substring(1,orderMap.keySet().toString().length()-1));
        for (Map.Entry<Long,Long> entry:orderMap.entrySet()) {
            if(stockMap.containsKey(entry.getKey())){
                if(stockMap.get(entry.getKey()) < entry.getValue()){
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    public Map<Long, Long> getStock(SQLiteDatabase db, String productIdString){

        Map<Long, Long> result = new HashMap<Long, Long>();
        String stockQuery = "SELECT " +
                DbContract.Stock.TABLE_NAME + "." + DbContract.Stock.COLUMN_NAME_QUANTITY + ", " +
                DbContract.Stock.TABLE_NAME + "." + DbContract.Stock.COLUMN_NAME_PRODUCT_ID +
                " FROM " + DbContract.Stock.TABLE_NAME +
                " WHERE " + DbContract.Stock.TABLE_NAME + "." + DbContract.Stock.COLUMN_NAME_PRODUCT_ID + " IN (" + productIdString + ")";
        Cursor cursor = db.rawQuery(stockQuery, null);
        while(cursor.moveToNext()){
            result.put(cursor.getLong(1), cursor.getLong(0));
        }
        return result;
    }

    public JSONObject toJSONObject(){
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("client_id", client.getId());
        jsonObject.put("order_date", getDateForSQL(getDate()));
        jsonObject.put("shipping_date", getDateForSQL(getShippingDate()));
        jsonObject.put("sum", getSum());
        jsonObject.put("comment", getComment());
        jsonObject.put("address_id", getAddressId());
        JSONArray orderLines = new JSONArray();
        for(OrderLine orderLine:lines){
            orderLines.add(orderLine.toJSONObject());
        }
        jsonObject.put("lines", orderLines);
        return jsonObject;
    }

    public ContentValues getContentValues(){
        ContentValues contentValues = new ContentValues();
        contentValues.put(DbContract.Orders.COLUMN_NAME_DATE, Order.getDateForSQL(getDate()));
        contentValues.put(DbContract.Orders.COLUMN_NAME_SHIPPING_DATE, Order.getDateForSQL(getShippingDate()));
        contentValues.put(DbContract.Orders.COLUMN_NAME_SUM, getSum());
        contentValues.put(DbContract.Orders.COLUMN_NAME_COMMENT, getComment());
        contentValues.put(DbContract.Orders.COLUMN_NAME_CLIENT_ID, getClient().getId());
        contentValues.put(DbContract.Orders.COLUMN_NAME_ADDRESS_ID, getAddressId());
        return contentValues;
    }
}
