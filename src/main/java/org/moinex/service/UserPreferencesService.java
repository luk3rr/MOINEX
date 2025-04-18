/*
 * Filename: UserPreferencesService.java
 * Created on: March 15, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import org.springframework.stereotype.Service;

@Service
public class UserPreferencesService
{
    private boolean hideMonetaryValues = false;

    public boolean hideMonetaryValues()
    {
        return this.hideMonetaryValues;
    }

    public boolean showMonetaryValues()
    {
        return !this.hideMonetaryValues;
    }

    public void toggleHideMonetaryValues()
    {
        this.hideMonetaryValues = !this.hideMonetaryValues;
    }
}
