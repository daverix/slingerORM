package net.daverix.slingerorm.compiler;

import net.daverix.slingerorm.annotation.DatabaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by daverix on 2/1/14.
 */
@DatabaseEntity
public class Sample {
    public String Id;
    public BigDecimal BigNumber;
    public Date Date;
}
