/*
 * Filename: CalculatorService.java
 * Created on: January  4, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

/**
 * Service class for the Calculator interface
 *
 * Stores the result of the last calculation and allows it to be accessed by other
 * classes
 */
@Service
@Getter
@Setter
public class CalculatorService
{
    private String result;

    public CalculatorService()
    {
        result = null;
    }
}
