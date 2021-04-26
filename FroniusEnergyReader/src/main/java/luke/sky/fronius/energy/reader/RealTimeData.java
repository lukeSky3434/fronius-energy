/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package luke.sky.fronius.energy.reader;

/**
 *
 * @author pendl2
 */
public class RealTimeData
{
		public String name;
		public String device;
		public long value;
		public String unit;

		@Override
		public String toString()
		{
				return "RealTimeData " + name + ": Device " + device + ", value: " + value + " " + unit;
		}
}
