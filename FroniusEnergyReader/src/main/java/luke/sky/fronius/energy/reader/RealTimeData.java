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
