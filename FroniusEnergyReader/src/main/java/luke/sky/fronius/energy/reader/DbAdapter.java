package luke.sky.fronius.energy.reader;


public interface DbAdapter
{
	public void sendMeasurement(
	 final String category,
	 final String name,
	 final Long value,
	 final String unit,
	 final String equipment,
	 final String area
	);
}
