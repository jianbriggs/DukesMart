package com.ruinscraft.dukesmart;

import java.util.Date;

public class IncomeDateWrapper {
	
	private final int income;
	private final Date date;
	public static int MAX_DAYS = 45;
	public static long MAX_DAYS_SECONDS = MAX_DAYS * 24 * 60 * 60;
	
	public IncomeDateWrapper(int income, Date date) {
		this.income = income;
		this.date = date;
	}
	
	public int getIncome() {
		return this.income;
	}
	
	public Date getDate() {
		return this.date;
	}
	
	public boolean dateIsExpired() {
		if(this.date != null) {
			long currentTime = System.currentTimeMillis();
			long dateInMillis = date.getTime();
			long difference = currentTime - dateInMillis;
			// 45 days is 3888000 seconds
			return (difference / 1000) >= MAX_DAYS_SECONDS;
		}
		else return false;
	}
	
	public int daysLeftBeforeExpire() {
		if(this.date != null) {
			long currentTime = System.currentTimeMillis();
			long dateInMillis = date.getTime();
			long difference = currentTime - dateInMillis;
			// 45 days is 3888000 seconds
			long days = (difference / 1000) / (60 * 60 * 24);
			
			return (int) (MAX_DAYS - days);
		}
		return -1;
	}
}
