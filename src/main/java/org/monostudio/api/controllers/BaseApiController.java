package org.monostudio.api.controllers;

import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Base controller — all API controllers extend this to get the /api prefix.
 */
@RequestMapping("/api")
public abstract class BaseApiController {
}
